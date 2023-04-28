import javax.swing.*;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.*;

public class FileTransferServer {
    private static final int PORT_NUMBER = 4444;
    private final ArrayList<ClientThread> clients;// 접속한 클라이언트들의 소켓 정보를 저장하는 ArrayList

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

            // 클라이언트 정보 통보
            out.println("You are connected to the server.");

            // 파일 전송 요청 대기
            while (true) {
                String line = in.readLine();
                if (line == null || line.isEmpty()) {
                    break;
                } else if (line.equals("SEND_FILE")) {
                    // 클라이언트로부터 파일 전송 요청을 받음
                    out.println("Please select a file to send.");

                    // 파일 선택 창을 띄우고, 선택된 파일을 서버에 전송
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setMultiSelectionEnabled(false);
                    int result = fileChooser.showOpenDialog(null);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileChooser.getSelectedFile();

                        // 파일 내용 전송
                        try (BufferedInputStream bis = new BufferedInputStream(clientSocket.getInputStream());
                             FileOutputStream fos = new FileOutputStream(selectedFile)) {

                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = bis.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                        }

                        // 파일 전송 완료 메시지 전송
                        out.println("File transfer completed.");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error handling client: " + e);
        } finally {
            try {
                // 클라이언트 소켓 닫기
                server.removeClient(this); // 클라이언트 스레드 정보를 ArrayList에서 삭제
                System.out.println("closing client socket");
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e);
            }
        }
    }
}

//위 코드에서는 `ClientThread` 클래스에 `removeClient` 메소드를 추가하여,
//클라이언트가 접속을 끊었을 때 서버에서 해당 클라이언트 스레드 정보를 제거하는 코드를 추가했습니다.
//        또한 클라이언트로부터 "SEND_FILE"이라는 문자열을 전송받으면 파일 선택 창을 띄우고,
//        선택된 파일을 서버에 전송하는 방식으로 변경하였습니다. 파일 전송이 완료되면
//        클라이언트로부터 "File sent successfully."라는 메시지를 수신받고, 서버 측에서도 해당 메시지를 출력합니다.
