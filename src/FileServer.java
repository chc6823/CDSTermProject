import java.io.*;
import java.net.*;
import java.util.*;

public class FileServer {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private String filePath = "C:\\Users\\chc68\\OneDrive\\바탕 화면\\server";

    public FileServer(int port, String filePath) {
        try {
            serverSocket = new ServerSocket(port);
            clients = new ArrayList<>();
            this.filePath = filePath;
            System.out.println("File server started on port " + port);
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
                broadcast("Client connected: " + client.getSocketAddress());
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
        private Socket socket;
        private BufferedReader input;
        private PrintWriter output;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        public void run() {
            try {
                String message;
                while ((message = input.readLine()) != null) {
                    if (message.startsWith("FILE:")) {
                        String[] parts = message.split(":");
                        String fileName = parts[1];
                        String fileContent = parts[2];
                        File file = new File(filePath + "/" + fileName);
                        FileWriter writer = new FileWriter(file);
                        writer.write(fileContent);
                        writer.close();
                        System.out.println("File saved: " + file.getAbsolutePath());
                    } else {
                        broadcast(message);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                    clients.remove(this);
                    broadcast("Client disconnected: " + getSocketAddress());
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

