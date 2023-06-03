import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileClient {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    // FileClient 클래스에 FileMetadata 객체의 리스트를 추가
    private List<FileMetadata> fileMetadataList;


    public FileClient(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connected to server " + serverAddress + ":" + serverPort);
            this.fileMetadataList = new ArrayList<>();
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
                        sendMessage("FILE:" + fileName + ":" + fileContent);
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

    public void deleteFile(String fileName) {
        for (Iterator<FileMetadata> iterator = fileMetadataList.iterator(); iterator.hasNext();) {
            FileMetadata metadata = iterator.next();
            if (metadata.getFileName().equals(fileName)) {
                iterator.remove();
                sendMessage("DELETE:" + fileName);
                System.out.println("File deleted successfully: " + fileName);
                break;
            }
        }
    }

    //서버로부터의 메시지를 처리하여 충돌을 검출하고 처리하는 handleServerMessage 메서드
    public void handleServerMessage(String message) {
        // 충돌 검출 및 처리 코드
        if (message.startsWith("META:")) {
            String[] parts = message.split(":");
            String fileName = parts[1];
            int serverLogicalClock = Integer.parseInt(parts[2]);

            for (FileMetadata metadata : fileMetadataList) {
                if (metadata.getFileName().equals(fileName)) {
                    if (metadata.getLogicalClock() <= serverLogicalClock) {
                        // 클라이언트와 서버의 논리적 시계가 동일하거나, 서버의 논리적 시계가 더 크다면 충돌이 발생
                        System.out.println("Conflict detected for file: " + fileName);
                        // 충돌 처리 코드 필요
                    }
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        FileClient client = new FileClient("localhost", 12345);
        client.upload();
        client.disconnect();
    }
}
