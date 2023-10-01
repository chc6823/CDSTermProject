import java.io.*;
import java.net.*;
import java.util.*;

public class FileServer {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private String baseDirectoryPath = "C:\\Users\\chc68\\OneDrive\\바탕 화면\\server";
    private Map<String, FileMetadata> fileMetadataMap;

    public FileServer(int port, String filePath) {
        try {
            serverSocket = new ServerSocket(port);
            clients = new ArrayList<>();
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
                broadcast("Client" + client.getClientId() + " connected.");
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
                break;
            }
        }
    }

    private synchronized void broadcast(String message) {
        System.out.println(message);
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    private class ClientHandler extends Thread {
        private final Socket socket;
        private String clientId;
        private BufferedReader input;
        private PrintWriter output;
        private String clientDirectoryPath;

        public ClientHandler(Socket socket) {
            this.socket = socket;

            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String message = input.readLine();
                if (message.startsWith("CLIENT_ID:")) {
                    clientId = message.substring("CLIENT_ID:".length());
                }
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }

            try {
                output = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }

            clientDirectoryPath = baseDirectoryPath + "\\Client " + clientId;
            File clientDirectory = new File(clientDirectoryPath);
            if (!clientDirectory.exists()) {
                if (clientDirectory.mkdir()) {
                    System.out.println("Directory created for client " + clientId + " at " + clientDirectoryPath);
                } else {
                    System.out.println("Failed to create directory for client " + clientId);
                }
            }
        }

        public String getClientId() {
            return clientId;
        }

        public void run() {
            String message;
            try {
                while ((message = input.readLine()) != null) {
                    System.out.println(message);

                    if (message.startsWith("CREATE:")) {
                        String[] parts = message.split(":");
                        String fileName = parts[1];
                        String fileContent = parts[2];

                        FileMetadata metadata = new FileMetadata("server_" + fileName, 1);
                        fileMetadataMap.put("server_" + fileName, metadata);

                        try (PrintWriter writer = new PrintWriter(new FileWriter(baseDirectoryPath + "\\" + "server_" + fileName))) {
                            writer.write(fileContent);
                            System.out.println("Server file created: server_"+ fileName);
                        } catch (IOException e) {
                            System.out.println("Error creating server file: " + e.getMessage());
                        }

                        try (PrintWriter writer = new PrintWriter(new FileWriter(clientDirectoryPath + "\\" + "client_" + fileName))) {
                            writer.write(fileContent);
                            System.out.println("Client file created: client_" +fileName);

                            FileMetadata clientMetadata = new FileMetadata("client_" + fileName, 1);
                            clientMetadata.incrementLogicalClock();
                            fileMetadataMap.put("client_" + fileName, clientMetadata);

                        } catch (IOException e) {
                            System.out.println("Error creating client file: " + e.getMessage());
                        }
                    } else if (message.startsWith("DELETE:")) {
                        String fileName = message.split(":")[1];
                        File fileToDelete = new File(clientDirectoryPath + "\\" + "client_" + fileName);
                        if (fileToDelete.exists()) {
                            if (fileToDelete.delete()) {
                                System.out.println("File deleted successfully: " + "client_" + fileName);
                                fileMetadataMap.remove("client_" + fileName);
                            } else {
                                System.out.println("Failed to delete the file: " + "client_" + fileName);
                            }
                        } else {
                            System.out.println("File not found: " + fileName);
                        }
                    } else if (message.startsWith("UPDATE:")) {
                        String[] parts = message.split(":");
                        String fileName = parts[1];
                        String fileContent = parts[2];
                        System.out.println("String fileName : "+fileName+" String fileContent: "+fileContent);

                        // Extract the pure file name without the "client_" prefix,Test5
                        String pureFileName = fileName.substring("client_".length());
                        System.out.println("pureFileName : "+pureFileName);

                        // Delete the existing server file
                        File serverFile = new File(baseDirectoryPath + "\\server_" + pureFileName);
                        if (serverFile.exists()) {
                            if (serverFile.delete()) {
                                System.out.println("Server file deleted: server_" + pureFileName);
                            } else {
                                System.out.println("Failed to delete server file: server_" + pureFileName);
                            }
                        }
                        // Create a new server file with the updated content
                        File newServerFile = new File(baseDirectoryPath + "\\server_" + pureFileName);
                        try (PrintWriter writer = new PrintWriter(new FileWriter(newServerFile))) {
                            writer.write(fileContent);
                            System.out.println("Server file created: server_" + pureFileName);
                        } catch (IOException e) {
                            System.out.println("Error creating server file: " + e.getMessage());
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

        public void sendMessage(String message) {
            output.println(message);
        }
    }

    public static void main(String[] args) {
        FileServer server = new FileServer(12345, "C:\\Users\\chc68\\OneDrive\\바탕 화면\\server");
        server.start();
    }
}
