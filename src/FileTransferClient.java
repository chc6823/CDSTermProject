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
        // 서버에 전송할 파일 선택
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        int result = fileChooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File[] selectedFiles = fileChooser.getSelectedFiles();

        // 선택한 파일들을 서버에 전송
        for (File file : selectedFiles) {
            // 파일 이름 전송
            Socket socket = new Socket(SERVER_ADDRESS, PORT_NUMBER);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
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
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println(in.readLine());

            // 소켓 닫기
            socket.close();
        }
    }

}
