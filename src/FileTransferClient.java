import javax.swing.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class FileTransferClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT_NUMBER = 4444;

    public static void main(String[] args) throws IOException {
        // 클라이언트 스레드 풀 생성
        ExecutorService clientThreadPool = Executors.newFixedThreadPool(10);

        while (true) {
            // 서버에 전송할 파일 선택
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            int result = fileChooser.showOpenDialog(null);
            if (result != JFileChooser.APPROVE_OPTION) {
                break;
            }
            File[] selectedFiles = fileChooser.getSelectedFiles();
            for (File file : selectedFiles) {
                System.out.println("File Name : "+file.getName());
            }

            // 선택한 파일들을 서버에 전송
            for (File file : selectedFiles) {
                // 파일 이름 전송
                Socket socket = new Socket(SERVER_ADDRESS, PORT_NUMBER);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(file.getName());

                // 파일 내용 전송
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    OutputStream os = socket.getOutputStream();
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                        System.out.println(bytesRead);
                    }
                    os.flush();
                    socket.shutdownOutput();
                }

                // 파일 전송 완료 메시지 수신
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String response;
                while ((response = in.readLine()) != null) {
                    System.out.println(response);
                }

                // 소켓 닫기
                socket.close();
            }
        }
    }
}
