import java.io.*;
import java.net.*;
import java.util.*;

public class FileServer {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;

    public FileServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            clients = new ArrayList<>();
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

    public void broadcast(String message) {
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
                while (true) {
                    String message = input.readLine();
                    if (message == null) {
                        break;
                    }
                    System.out.println("Received message from client " + socket.getRemoteSocketAddress() + ": " + message);
                }
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            } finally {
                clients.remove(this);
                broadcast("Client disconnected: " + getSocketAddress());
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }

        public void sendMessage(String message) {
            output.println(message);
        }

        public SocketAddress getSocketAddress() {
            return socket.getRemoteSocketAddress();
        }
    }

    public static void main(String[] args) {
        FileServer server = new FileServer(12345);
        server.start();
    }
}
