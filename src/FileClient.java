import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.net.*;
import java.util.*;

public class FileClient {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String baseDirectoryPath = "C:\\Users\\chc68\\OneDrive\\바탕 화면\\server";
    private String clientId;
    private String clientDirectoryPath;
    private List<FileMetadata> fileMetadataList;

    public FileClient(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connected to server " + serverAddress + ":" + serverPort);

            this.fileMetadataList = new ArrayList<>();

            clientId = UUID.randomUUID().toString();
            sendMessage("CLIENT_ID:" + clientId);

            clientDirectoryPath = baseDirectoryPath + "\\Client " + clientId;
            startFileUpdateDetectionThread(clientDirectoryPath);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void startFileUpdateDetectionThread(String directoryPath) {
        Thread thread = new Thread(() -> {
            try {
                File folder = new File(directoryPath);

                while (true) {
                    for (File file : folder.listFiles()) {
                        if (!file.getName().startsWith("client_")) continue;

                        // Check if the file exists in the file metadata list
                        FileMetadata metadata = getFileMetadata(file.getName());
                        if (metadata == null) { // File is newly created
                            // Add the file to the file metadata list
                            metadata = new FileMetadata(file.getName(), 0);
                            fileMetadataList.add(metadata);
                        } else if (file.lastModified() > metadata.getLogicalClock()) { // File is modified
                            // Update the logical clock of the file metadata
                            metadata.setLogicalClock(file.lastModified());

                            // Wait for 2 seconds after the modification
                            Thread.sleep(2000);

                            // Update the file
                            updateFile(file);
                        }
                    }

                    Thread.sleep(500);
                }
            } catch (InterruptedException | NullPointerException e) {
                e.printStackTrace();
            }
        });

        thread.start();
    }

    private FileMetadata getFileMetadata(String fileName) {
        for (FileMetadata metadata : fileMetadataList) {
            if (metadata.getFileName().equals(fileName)) {
                return metadata;
            }
        }
        return null;
    }


    private void updateFile(File file) {
        try {
            String fileName = file.getName();
            String absoluteFilePath = file.getAbsolutePath();

            BufferedReader br = new BufferedReader(new FileReader(absoluteFilePath));
            String line;
            StringBuilder sb = new StringBuilder();

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\r\n"); // Use "\r\n" as the line separator for Windows
            }
            String fileContent = sb.toString();
            System.out.println("Client's Filecontent : "+fileContent);
            br.close();

            sendMessage("UPDATE:" + fileName + ":" + fileContent); // Use fileName instead of pureFileName
            System.out.println("File updated successfully: " + fileName);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    public void disconnect() {
        try {
            socket.close();
            System.out.println("Disconnected from server");
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        output.println(message);
        for (FileMetadata metadata : fileMetadataList) {
            output.println("META:" + metadata.getFileName() + ":" + metadata.getLogicalClock());
        }
    }

    public void upload() {
        while (true) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
            int result = fileChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                File[] files = fileChooser.getSelectedFiles();
                for (File file : files) {
                    if (!file.exists()) {
                        System.out.println("Error: File not found");
                        continue;
                    }
                    try {
                        String fileName = file.getName();
                        String absoluteFilePath = file.getAbsolutePath();

                        BufferedReader br = new BufferedReader(new FileReader(absoluteFilePath));
                        StringBuilder sb = new StringBuilder();
                        String line;

                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                            sb.append("\r\n"); // Use "\r\n" as the line separator for Windows
                        }
                        br.close();
                        String fileContent = sb.toString();

                        sendMessage("CREATE:" + fileName + ":" + fileContent);

                        System.out.println("File uploaded successfully: " + fileName);
                    } catch (IOException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
            } else {
                break;
            }
        }
    }

    public static void main(String[] args) {
        FileClient client = new FileClient("localhost", 12345);
        //client.upload();
        client.disconnect();
    }
}
