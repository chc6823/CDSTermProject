import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileTransferTest {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT_NUMBER = 4444;

    public static void main(String[] args) throws IOException {
        // 클라이언트 스레드 풀 생성
        ExecutorService clientThreadPool = Executors.newFixedThreadPool(5);

        // 다수의 클라이언트 접속 및 해제를 테스트
        for (int i = 0; i < 5; i++) {
            final int index = i;
            clientThreadPool.execute(() -> {
                try {
                    // 클라이언트 소켓 생성
                    Socket socket = new Socket(SERVER_ADDRESS, PORT_NUMBER);
                    System.out.println("Client " + index + " connected to server.");

                    // 서버에 전송할 파일 선택
                    File selectedFile = new File("file_" + index + ".txt");
                    selectedFile.createNewFile();

                    // 파일 이름 전송
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(selectedFile.getName());

                    // 파일 내용 전송
                    FileInputStream fis = new FileInputStream(selectedFile);
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
                    System.out.println("Client " + index + " disconnected from server.");
                } catch (IOException e) {
                    System.out.println("Error handling client: " + e);
                }
            });
        }

        // 클라이언트 스레드 풀 종료
        clientThreadPool.shutdown();
    }
}
