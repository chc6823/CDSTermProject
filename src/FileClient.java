import java.io.*;
import java.util.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileClient {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String baseDirectoryPath = "C:\\Users\\chc68\\OneDrive\\바탕 화면\\server";
    private String clientId;
    private String clientDirectoryPath = baseDirectoryPath + "\\Client" + clientId;
    // Add a new private field to store the last modified time of files in the client directory
    private Map<String, Long> lastModifiedTimes;

    // FileClient 클래스에 FileMetadata 객체의 리스트를 추가
    private List<FileMetadata> fileMetadataList;


    public FileClient(String serverAddress, int serverPort) {
        try {
            //initialize socket,input/putput
            socket = new Socket(serverAddress, serverPort);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connected to server " + serverAddress + ":" + serverPort);

            this.fileMetadataList = new ArrayList<>();
            this.lastModifiedTimes = new HashMap<>();

            // 클라이언트 ID 생성
            clientId = UUID.randomUUID().toString();
            // 서버에 클라이언트 ID 전송
            sendMessage("CLIENT_ID:" + clientId);

            compareAndSendUpdateRequests();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void compareAndSendUpdateRequests() {
        File[] clientFiles = new File(clientDirectoryPath).listFiles();
        if (clientFiles != null) {
            for (File clientFile : clientFiles) {
                String fileName = clientFile.getName();
                long clientModifiedTime = clientFile.lastModified();

                File serverFile = new File(baseDirectoryPath + "\\" +"server_"+fileName);
                long serverModifiedTime = serverFile.lastModified();

                if (serverModifiedTime != clientModifiedTime) {
                    // Server file is more recent, send an update request
                    updateFile(serverFile);
                }
            }
        }
    }

    private void updateFile(File file) {
        try {
            FileInputStream fileInput = new FileInputStream(file);
            ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInput.read(buffer)) != -1) {
                byteArrayOutput.write(buffer, 0, bytesRead);
            }
            fileInput.close();
            byteArrayOutput.flush();
            byte[] bytes = byteArrayOutput.toByteArray();
            String fileName = file.getName();
            String fileContent = new String(bytes);

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
        //메타데이터를 함께 전송
        for (FileMetadata metadata : fileMetadataList) {
            output.println("META:" + metadata.getFileName() + ":" + metadata.getLogicalClock());
        }
    }

    public void upload() {
        while (true) {
            //selecting File in JFilechooser
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
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = fileInput.read(buffer)) != -1) {
                            byteArrayOutput.write(buffer, 0, bytesRead);
                        }

                        fileInput.close();
                        byteArrayOutput.flush();
                        byte[] bytes = byteArrayOutput.toByteArray();

                        String fileName = file.getName();
                        String fileContent = new String(bytes);
                        fileMetadataList.add(new FileMetadata(fileName, 1)); // 초기 논리 시계는 1로 설정

                        // Send a request to the server to create the file in both server and client directories
                        sendMessage("CREATE:" + fileName + ":" + fileContent);
                        System.out.println("File uploaded successfully: " + fileName);
                    } catch (IOException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
            } else {
                break;  // 사용자가 파일 선택을 취소하면 while문을 종료
            }
        }
    }

    // 클라이언트 ID getter
    public String getClientId() {
        return clientId;
    }

    public static void main(String[] args) {
        FileClient client = new FileClient("localhost", 12345);
        client.upload();
        client.disconnect();
    }
}
