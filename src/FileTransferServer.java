import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class FileTransferServer {
    private static final int PORT_NUMBER = 4444;
    private static final String FILE_DIRECTORY = "./received_files/";

    public static void main(String[] args) throws IOException {
        // 서버 소켓 생성
        ServerSocket serverSocket = new ServerSocket(PORT_NUMBER);
        System.out.println("Server started on port " + PORT_NUMBER);

        // 클라이언트 스레드 풀 생성
        ExecutorService clientThreadPool = Executors.newFixedThreadPool(10);

        // 클라이언트 접속 대기
        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected: " + clientSocket.getInetAddress());

            // 새로운 클라이언트의 소켓을 이용하여 클라이언트 쓰레드 생성
            Runnable clientThread = new ClientThread(clientSocket);
            clientThreadPool.execute(clientThread);
        }
    }
}

class ClientThread implements Runnable {
    private Socket clientSocket;

    public ClientThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void run() {
        try {
            // 클라이언트와의 입출력 스트림 생성
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            // 클라이언트가 전송한 파일을 서버에 저장
            String fileName = in.readLine();
            // 파일이 있는 디렉토리 경로를 지정
            File file = new File("C:\\Users\\chc68\\OneDrive\\문서\\" + fileName);
            FileInputStream fis = new FileInputStream(file);

            File outputFile = new File("C:\\Users\\chc68\\OneDrive\\바탕 화면\\컴공\\5-1\\" +
                    "협동분산시스템" + fileName);
            FileOutputStream fos = new FileOutputStream(outputFile);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                clientSocket.getOutputStream().write(buffer, 0, bytesRead);
            }
            fis.close();

            byte[] buffer2 = new byte[1024];
            int bytesRead2;
            while ((bytesRead2= clientSocket.getInputStream().read(buffer)) != -1) {
                fos.write(buffer2, 0, bytesRead2);
            }
            fos.close();

            // 파일 전송 완료 메시지 전송
            out.println("File " + fileName + " sent successfully.");
        } catch (IOException e) {
            System.out.println("Error handling client: " + e);
        } finally {
            try {
                // 클라이언트 소켓 닫기
                clientSocket.close();
                System.out.println("Client disconnected: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e);
            }
        }
    }
}

