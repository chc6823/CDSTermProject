import java.io.*;
import java.net.*;
import java.util.*;

public class FileServer {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private String filePath = "C:\\Users\\chc68\\OneDrive\\바탕 화면\\server";
    private String baseDirectoryPath = "C:\\Users\\chc68\\OneDrive\\바탕 화면\\server";

    private Map<String, FileMetadata> fileMetadataMap;

    public FileServer(int port, String filePath) {
        try {
            serverSocket = new ServerSocket(port);
            clients = new ArrayList<>();
            this.filePath = filePath;
            System.out.println("File server started on port " + port);
            this.fileMetadataMap = new HashMap<>();
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void start() {
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket);
                clients.add(client);
                client.start();
                broadcast("Client"+client.getClientId()+" connected.");
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                break;
            }
        }
    }

    public void stop() {
        try {
            serverSocket.close();
            System.out.println("File server stopped");
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private synchronized void broadcast(String message) {
        System.out.println(message);
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private class ClientHandler extends Thread {
        private static int nextClientId = 1;
        private final Socket socket;
        private final int clientId;
        private BufferedReader input;
        private PrintWriter output;
        private String clientDirectoryPath;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            clientId = nextClientId++;
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
            clientDirectoryPath = baseDirectoryPath + "\\Client" + clientId;
            File clientDirectory = new File(clientDirectoryPath);
            if (!clientDirectory.exists()) {
                if (clientDirectory.mkdir()) {
                    System.out.println("Directory created for client " + clientId + " at " + clientDirectoryPath);
                } else {
                    System.out.println("Failed to create directory for client " + clientId);
                }
            }
        }

        public int getClientId() {
            return clientId;
        }

        public void run() {
            String message;
            try {
                while ((message = input.readLine()) != null) {
                    System.out.println("Received: " + message);

                    if (message.startsWith("FILE:")) {
                        // 파일 업로드 요청 처리 코드 필요
                    } else if (message.startsWith("DELETE:")) {
                        String fileName = message.split(":")[1];
                        fileMetadataMap.remove(fileName);
                        System.out.println("File deleted successfully: " + fileName);
                    } else if (message.startsWith("META:")) {
                        String[] parts = message.split(":");
                        String fileName = parts[1];
                        int clientLogicalClock = Integer.parseInt(parts[2]);
                        FileMetadata metadata = fileMetadataMap.get(fileName);

                        if (metadata != null) {
                            if (metadata.getLogicalClock() > clientLogicalClock) {
                                // 서버의 논리적 시계가 클라이언트의 논리적 시계보다 크다면 충돌이 발생
                                System.out.println("Conflict detected for file: " + fileName);
                                // 충돌 처리 코드 필요
                            } else {
                                // 논리적 시계 업데이트
                                metadata.incrementLogicalClock();
                            }
                        } else {
                            fileMetadataMap.put(fileName, new FileMetadata(fileName, clientLogicalClock));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                    clients.remove(this);
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }

        }


        public SocketAddress getSocketAddress() {
            return socket.getRemoteSocketAddress();
        }

        public void sendMessage(String message) {
            output.println(message);
        }
    }

    public static void main(String[] args) {
        FileServer server = new FileServer(12345, "C:\\Users\\chc68\\OneDrive\\바탕 화면\\server");
        server.start();
    }
}

