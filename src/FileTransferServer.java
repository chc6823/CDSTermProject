import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.*;

public class FileTransferServer {
    private static final int PORT_NUMBER = 4444;
    private static final String FILE_PATH
            = "C:\\Users\\chc68\\OneDrive\\바탕 화면\\컴공\\5-1\\협동분산시스템\\CDSTermProject3";;
            // 저장할 파일 경로

    private final ArrayList<ClientThread> clients; // 접속한 클라이언트들의 소켓 정보를 저장하는 ArrayList

    public FileTransferServer() {
        clients = new ArrayList<>();
    }

    public static void main(String[] args) throws IOException {
        FileTransferServer server = new FileTransferServer();
        server.startServer();
    }

    public void startServer() throws IOException {
        // 서버 소켓 생성
        try (ServerSocket serverSocket = new ServerSocket(PORT_NUMBER)) {
            System.out.println("Server started on port " + PORT_NUMBER);

            // 클라이언트 스레드 풀 생성
            ExecutorService clientThreadPool = Executors.newFixedThreadPool(5);

            // 클라이언트 접속 대기
            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // 새로운 클라이언트의 소켓을 이용하여 클라이언트 쓰레드 생성
                Runnable clientThread = new ClientThread(clientSocket, this);
                clientThreadPool.execute(clientThread);
            }

            // 서버 종료 처리
            clientThreadPool.shutdown();

        }
    }

    public void saveFile(String fileName, byte[] content) throws IOException {
        // 파일 생성
        File file = new File(FILE_PATH + fileName);
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            bos.write(content);
            bos.flush();
        }
    }

    public void removeClient(ClientThread clientThread) {
        if (clients.contains(clientThread)) {
            clients.remove(clientThread);
            System.out.println("Client disconnected: " + clientThread.getClientSocket().getInetAddress());
        }
    }

}

class ClientThread implements Runnable {
    private final Socket clientSocket;
    private final FileTransferServer server;

    public ClientThread(Socket clientSocket, FileTransferServer server) {
        this.clientSocket = clientSocket;
        this.server = server;
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public void run() {
        try {
            // 클라이언트와의 입출력 스트림 생성
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());

            // 클라이언트 정보 통보
            out.println("You are connected to the server.");

            // 파일 전송 요청 대기
            while (true) {
                String request = dataInputStream.readUTF();
                if (request.equals("SEND_FILE")) {
                    // 클라이언트로부터 파일 전송 요청을 받음
                    out.println("Please select a file to send.");

                    // 파일 이름 받음
                    String fileName = dataInputStream.readUTF();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    byte[] content = baos.toByteArray();
                    server.saveFile(fileName, content);

                    // 파일 전송 완료 메시지 전송
                    out.println("File transfer completed.");
                } else {
                    // 잘못된 요청인 경우
                    out.println("Invalid request.");
                }
            }
        } catch (IOException e) {
            System.out.println("Error handling client: " + e);
        } finally {
            try {
                // 클라이언트 소켓 닫기
                server.removeClient(this); // 클라이언트 스레드 정보를 ArrayList에서 삭제
                System.out.println("closing client socket: ");
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e);
            }
        }
    }

}

