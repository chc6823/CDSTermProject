import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileClient {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String baseDirectoryPath = "C:\\Users\\chc68\\OneDrive\\바탕 화면\\server";
    private String clientId;
    private String clientDirectoryPath;
    private Map<String, Long> lastModifiedTimes;
    private List<FileMetadata> fileMetadataList;

    public FileClient(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connected to server " + serverAddress + ":" + serverPort);

            this.fileMetadataList = new ArrayList<>();
            this.lastModifiedTimes = new HashMap<>();

            clientId = UUID.randomUUID().toString();
            sendMessage("CLIENT_ID:" + clientId);

            clientDirectoryPath = baseDirectoryPath + "\\Client " + clientId;
            startFileUpdateDetectionThread();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void startFileUpdateDetectionThread() {
        Thread fileUpdateDetectionThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);

                    File[] files = new File(clientDirectoryPath).listFiles();
                    if (files != null) {
                        for (File file : files) {
                            long lastModifiedTime = file.lastModified();
                            String fileName = file.getName();

                            if (lastModifiedTimes.containsKey(fileName)) {
                                long storedTime = lastModifiedTimes.get(fileName);
                                if (lastModifiedTime > storedTime) {
                                    updateFile(file);
                                    lastModifiedTimes.put(fileName, lastModifiedTime);
                                }
                            } else {
                                lastModifiedTimes.put(fileName, lastModifiedTime);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("File update detection thread interrupted: " + e.getMessage());
                    break;
                }
            }
        });

        fileUpdateDetectionThread.setDaemon(true);
        fileUpdateDetectionThread.start();
    }
    private void updateFile(File file) {
        try {
            FileInputStream fileInput = new FileInputStream(file);
            ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
            byte[] buffer = new byte[10*1024 * 1024];
            int bytesRead;
            while ((bytesRead = fileInput.read(buffer)) != -1) {
                byteArrayOutput.write(buffer, 0, bytesRead);
            }
            fileInput.close();
            byteArrayOutput.flush();
            byte[] bytes = byteArrayOutput.toByteArray();
            String fileName = file.getName();
            String fileContent = new String(bytes);

            // Update the server file
            File serverFile = new File(baseDirectoryPath + "\\server_" + fileName);
            try (PrintWriter writer = new PrintWriter(new FileWriter(serverFile))) {
                writer.write(fileContent);
                System.out.println("Server file updated: server_" + fileName);
            } catch (IOException e) {
                System.out.println("Error updating server file: " + e.getMessage());
            }
            sendMessage("UPDATE:" + fileName + ":" + fileContent);
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
                        FileInputStream fileInput = new FileInputStream(file);
                        ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
                        byte[] buffer = new byte[10*1024*1024];
                        int bytesRead;
                        while ((bytesRead = fileInput.read(buffer)) != -1) {
                            byteArrayOutput.write(buffer, 0, bytesRead);
                        }

                        fileInput.close();
                        byteArrayOutput.flush();
                        byte[] bytes = byteArrayOutput.toByteArray();

                        String fileName = file.getName();
                        String fileContent = new String(bytes);
                        fileMetadataList.add(new FileMetadata(fileName, 1));

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

    public String getClientId() {
        return clientId;
    }

    public static void main(String[] args) {
        FileClient client = new FileClient("localhost", 12345);
        client.upload();
        client.disconnect();
    }
}
