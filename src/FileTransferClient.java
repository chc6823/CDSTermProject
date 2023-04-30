import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

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
                sendFilesToServer(selectedFiles);
            } else {
                break; // JFileChooser 창에서 Cancel 버튼을 누르면 무한루프를 빠져나감
            }
        }
    }

    private static void sendFilesToServer(File[] files) throws IOException {
        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.connect(new InetSocketAddress(SERVER_ADDRESS, PORT_NUMBER));
            System.out.println("Connected to server");

            for (File file : files) {
                // 파일 이름 및 내용 전송
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    buffer.put(file.getName().getBytes());
                    buffer.putLong(file.length());
                    buffer.flip();
                    socketChannel.write(buffer);

                    buffer.clear();

                    byte[] fileContent = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(fileContent)) != -1) {
                        buffer.put(fileContent, 0, bytesRead);
                        buffer.flip();
                        while (buffer.hasRemaining()) {
                            socketChannel.write(buffer);
                        }
                        buffer.clear();
                    }

                    // 파일 전송 완료 메시지 수신
                    buffer = ByteBuffer.allocate(1024);
                    while (socketChannel.read(buffer) != -1) {
                        buffer.flip();
                        System.out.println(new String(buffer.array(), 0, buffer.limit()));
                        buffer.clear();
                    }
                } catch (IOException e) {
                    System.out.println("Error handling file: " + e);
                }
            }
        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e);
        }
        // 파일 전송이 끝나면 파일 선택 창 다시 띄우기
        selectFilesAndSendToServer();
    }
}
