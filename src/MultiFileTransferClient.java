import javax.swing.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MultiFileTransferClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT_NUMBER = 4444;

    public static void main(String[] args) throws IOException {
        ExecutorService clientThreadPool = Executors.newFixedThreadPool(10);

        while (true) {
            // 파일 선택 대화상자 출력
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            int result = fileChooser.showOpenDialog(null);
            if (result != JFileChooser.APPROVE_OPTION) {
                continue;
            }
            File[] selectedFiles = fileChooser.getSelectedFiles();

            // 선택한 파일들을 서버로 전송
            Socket socket = new Socket(SERVER_ADDRESS, PORT_NUMBER);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            for (File file : selectedFiles) {
                // 파일 이름 전송
                out.println(file.getName());

                // 파일 내용 전송
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    socket.getOutputStream().write(buffer, 0, bytesRead);
                }
                fis.close();

                // 파일 전송 완료 메시지 수신
                System.out.println(in.readLine());
            }

            // 소켓 닫기
            socket.close();
        }
    }
}
