import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MultiFileTransferServer {
    private static final int PORT_NUMBER = 4444;
    private static final int THREAD_POOL_SIZE = 10;

    // 접속한 클라이언트 정보를 저장할 맵
    private static Map<Socket, String> clientMap = new HashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT_NUMBER);
        System.out.println("Server started on port " + PORT_NUMBER);

        // 클라이언트 스레드 풀 생성
        ExecutorService clientThreadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        while (true) {
            // 클라이언트 접속 대기
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected: " + clientSocket.getInetAddress());

            // 새로운 클라이언트 쓰레드 생성 및 실행
            clientThreadPool.execute(() -> handleClient(clientSocket));
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            // 클라이언트와의 입출력 스트림 생성
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            // 클라이언트 로그인
            out.println("Welcome to the file transfer server!");
            out.println("Enter your name:");
            String clientName = in.readLine();
            clientMap.put(clientSocket, clientName);
            System.out.println(clientName + " logged in.");

            // 다른 클라이언트에게 새로운 클라이언트 정보 전송
            for (Socket socket : clientMap.keySet()) {
                if (socket != clientSocket) {
                    PrintWriter otherOut = new PrintWriter(socket.getOutputStream(), true);
                    otherOut.println(clientName + " has joined the server.");
                }
            }

            // 클라이언트와의 대화 처리
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.equalsIgnoreCase("quit")) {
                    break;
                }
            }

            // 클라이언트 접속 해제 처리
            String disconnectedClientName = clientMap.get(clientSocket);
            clientMap.remove(clientSocket);
            clientSocket.close();
            System.out.println(disconnectedClientName + " disconnected.");

            // 다른 클라이언트에게 클라이언트 해제 정보 전송
            for (Socket socket : clientMap.keySet()) {
                PrintWriter otherOut = new PrintWriter(socket.getOutputStream(), true);
                otherOut.println(disconnectedClientName + " has left the server.");
            }
        } catch (IOException e) {
            System.out.println("Error handling client: " + e);
        }
    }
}
