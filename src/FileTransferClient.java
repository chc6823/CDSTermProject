import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileTransferClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT_NUMBER = 4444;

    public static void main(String[] args) throws IOException {
        // 파일 선택 창을 띄우고, 선택된 파일들을 서버에 전송
        selectFilesAndSendToServer();
    }

    private static void selectFilesAndSendToServer() throws IOException {
        while (true) {
            // 서버에 전송할 파일 선택
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = fileChooser.getSelectedFiles();
                for (File file : selectedFiles) {
                    // 파일 이름 및 내용 전송
                    try (Socket socket = new Socket(SERVER_ADDRESS, PORT_NUMBER);
                         DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
                        out.writeUTF(file.getName());
                        out.writeLong(file.length());

                        FileInputStream fileInputStream = new FileInputStream(file);
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        fileInputStream.close();

                        // 파일 전송 완료 메시지 수신
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String response;
                        while ((response = in.readLine()) != null) {
                            System.out.println(response);
                        }
                    } catch (IOException e) {
                        System.out.println("Error handling file: " + e);
                    }
                }
            } else {
                break; // JFileChooser 창에서 Cancel 버튼을 누르면 무한루프를 빠져나감
            }
        }
    }
}
