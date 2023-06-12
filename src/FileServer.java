import java.io.*;
import java.net.*;
import java.util.*;

public class FileServer {
    private ServerSocket serverSocket;
    private List<ClientHandler> clients;
    private String baseDirectoryPath = "C:\\Users\\chc68\\OneDrive\\바탕 화면\\server";
    private Map<String, FileMetadata> fileMetadataMap;

    //constructor
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
                client.start(); //thread start
                broadcast("Client"+client.getClientId()+" connected.");
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

            // 클라이언트로부터 클라이언트 ID 수신
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String message = input.readLine();
                if (message.startsWith("CLIENT_ID:")) {
                    clientId = message.substring("CLIENT_ID:".length());
                }
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                System.out.println("Error: " + e.getMessage());
            }
            //Creating Repository for client (in Folder)
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

        public String getClientId() {
            return clientId;
        }

        public void run() {
            String message;
            try {
                while ((message = input.readLine()) != null) {
                    System.out.println("Received: " + message);

                    if (message.startsWith("CREATE:")) {

                        String[] parts = message.split(":");
                        String fileName = parts[1];
                        String fileContent = parts[2];

                        FileMetadata metadata = new FileMetadata(fileName, 1);
                        fileMetadataMap.put(fileName, metadata);

                        // Create the file in the server directory
                        try (PrintWriter writer = new PrintWriter(new FileWriter(baseDirectoryPath+"\\"+fileName))) {
                            writer.write(fileContent);
                            System.out.println("Server file created: "+ fileName);
                        } catch (IOException e) {
                            System.out.println("Error creating server file: " + e.getMessage());
                        }
                        // Create the file in the client directory
                        try (PrintWriter writer = new PrintWriter(new FileWriter(clientDirectoryPath + "\\" + fileName))) {
                            writer.write(fileContent);
                            System.out.println("Client file created: " + fileName);
                        } catch (IOException e) {
                            System.out.println("Error creating client file: " + e.getMessage());
                        }
                    }
                    else if (message.startsWith("DELETE:")) {

                        //Delete request from client.
                        String fileName = message.split(":")[1];
                        fileMetadataMap.remove(fileName);
                        File fileToDelete = new File(clientDirectoryPath +"\\"+fileName);
                        if (fileToDelete.exists()) {
                            if (fileToDelete.delete()) {
                                System.out.println("File deleted successfully: " + fileName);
                            } else {
                                System.out.println("Failed to delete the file: " + fileName);
                            }
                        } else {
                            System.out.println("File not found: " + fileName);
                        }
                    }
                    else if (message.startsWith("UPDATE:")) {
                        String[] parts = message.split(":");
                        String fileName = parts[1];
                        String fileContent = parts[2];

                        FileMetadata metadata = fileMetadataMap.get(fileName);

                        if (metadata != null) {
                            if (metadata.getLogicalClock() > clientLogicalClock) {
                                // Conflict detected, delete the server file
                                File serverFile = new File(baseDirectoryPath+"\\"+fileName);
                                if (serverFile.exists()) {
                                    if (serverFile.delete()) {
                                        System.out.println("Server file deleted due to conflict: " + fileName);
                                    } else {
                                        System.out.println("Failed to delete server file: " + fileName);
                                    }
                                } else {
                                    System.out.println("Server file not found : "+fileName);
                                }
                            } else {
                                // Update the file content in both the server and client directories
                                try (PrintWriter serverWriter = new PrintWriter(new FileWriter(baseDirectoryPath+"\\"+fileName));
                                     PrintWriter clientWriter = new PrintWriter(new FileWriter(clientDirectoryPath + "\\" + fileName))) {
                                    serverWriter.write(fileContent);
                                    clientWriter.write(fileContent);

                                    // Update the logical clock
                                    metadata.incrementLogicalClock();
                                    System.out.println("File updated: " + fileName);
                                } catch (IOException e) {
                                    System.out.println("Error updating file: " + e.getMessage());
                                }
                            }
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

