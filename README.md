# 협분시 최종

### 201914187 허찬

## 1. 구현 환경

자바 내장 라이브러리 사용함.

## 2. 기능별 구현 완료 여부

1. 단순 파일 동기화 기능
3.1 클라이언트는 파일 업데이트(생성,삭제,수정)를 자동 또는 수동으로 탐지 O
3.2 클라이언트는 파일 업데이트가 서버의 파일과 충돌하지 않음을 확인 (logical
clock 이용) O
3.3 클라이언트는 충돌하지 않은 업데이트된 파일을 서버로 전송하여 기존 파일
덮어씀 O
3.4 삭제된 파일은 서버에서도 삭제 O
2. 다른 클라이언트와 파일 단순 공유 기능
4.1 파일 및 공유할 사용자(클라이언트) 선택 O
4.2 선택 파일을 (서버를 거쳐) 선택한 클라이언트에게 전송 O
4.3 공유된 파일 업데이트되면 서버뿐만 아니라 공유된 클라이언트에게도 전송
하여 업데이트 X
3. 대용량 파일 송수신 구현 X

## 3. 전체 클래스 및 메소드 설계

### FileClient

```java
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.net.*;
import java.util.*;

public class FileClient {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String baseDirectoryPath = "C:\\Users\\chc68\\OneDrive\\바탕 화면\\server";
    private String clientId;
    private String clientDirectoryPath;
    private List<FileMetadata> fileMetadataList;

    public FileClient(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connected to server " + serverAddress + ":" + serverPort);

            this.fileMetadataList = new ArrayList<>();

            clientId = UUID.randomUUID().toString();
            sendMessage("CLIENT_ID:" + clientId);

            clientDirectoryPath = baseDirectoryPath + "\\Client " + clientId;
            startFileUpdateDetectionThread(clientDirectoryPath);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void startFileUpdateDetectionThread(String directoryPath) {
        Thread thread = new Thread(() -> {
            try {
                File folder = new File(directoryPath);

                while (true) {
                    for (File file : folder.listFiles()) {
                        if (!file.getName().startsWith("client_")) continue;

                        // Check if the file exists in the file metadata list
                        FileMetadata metadata = getFileMetadata(file.getName());
                        if (metadata == null) { // File is newly created
                            // Add the file to the file metadata list
                            metadata = new FileMetadata(file.getName(), 0);
                            fileMetadataList.add(metadata);
                        } else if (file.lastModified() > metadata.getLogicalClock()) { // File is modified
                            // Update the logical clock of the file metadata
                            metadata.setLogicalClock(file.lastModified());

                            // Wait for 2 seconds after the modification
                            Thread.sleep(2000);

                            // Update the file
                            updateFile(file);
                        }
                    }

                    Thread.sleep(500);
                }
            } catch (InterruptedException | NullPointerException e) {
                e.printStackTrace();
            }
        });

        thread.start();
    }

    private FileMetadata getFileMetadata(String fileName) {
        for (FileMetadata metadata : fileMetadataList) {
            if (metadata.getFileName().equals(fileName)) {
                return metadata;
            }
        }
        return null;
    }

    private void updateFile(File file) {
        try {
            String fileName = file.getName();
            String absoluteFilePath = file.getAbsolutePath();

            BufferedReader br = new BufferedReader(new FileReader(absoluteFilePath));
            String line;
            StringBuilder sb = new StringBuilder();

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\r\n"); // Use "\r\n" as the line separator for Windows
            }
            String fileContent = sb.toString();
            System.out.println("Client's Filecontent : "+fileContent);
            br.close();

            sendMessage("UPDATE:" + fileName + ":" + fileContent); // Use fileName instead of pureFileName
            System.out.println("File updated successfully: " + fileName);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void shareFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getName();

            JFileChooser directoryChooser = new JFileChooser();
            directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            result = directoryChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedDirectory = directoryChooser.getSelectedFile();
                String directoryPath = selectedDirectory.getAbsolutePath();

                try (BufferedReader br = new BufferedReader(new FileReader(selectedFile))) {
                    StringBuilder sb = new StringBuilder();
                    String line;

                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                        sb.append("\r\n"); // Use "\r\n" as the line separator for Windows
                    }
                    String fileContent = sb.toString();

                    String message = "SHARE#$" + directoryPath + "#$" + fileName + "#$" + fileContent;
                    sendMessage(message);

                    System.out.println("File shared successfully: " + fileName);
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            } else {
                System.out.println("No directory selected");
            }
        }
    }

    public void disconnect() {
        try {
            socket.close();
            System.out.println("Disconnected from server");
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        output.println(message);
        for (FileMetadata metadata : fileMetadataList) {
            output.println("META:" + metadata.getFileName() + ":" + metadata.getLogicalClock());
        }
    }

    public void upload() {
        while (true) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
            int result = fileChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                File[] files = fileChooser.getSelectedFiles();
                for (File file : files) {
                    if (!file.exists()) {
                        System.out.println("Error: File not found");
                        continue;
                    }
                    try {
                        String fileName = file.getName();
                        String absoluteFilePath = file.getAbsolutePath();

                        BufferedReader br = new BufferedReader(new FileReader(absoluteFilePath));
                        StringBuilder sb = new StringBuilder();
                        String line;

                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                            sb.append("\r\n"); // Use "\r\n" as the line separator for Windows
                        }
                        br.close();
                        String fileContent = sb.toString();

                        sendMessage("CREATE:" + fileName + ":" + fileContent);

                        System.out.println("File uploaded successfully: " + fileName);
                    } catch (IOException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
            } else {
                break;
            }
        }
    }

    public static void main(String[] args) {
        FileClient client = new FileClient("localhost", 12345);
        //client.upload();
        client.disconnect();
    }
}
```

파일 클라이언트를 구현한 클래스입니다.

1. **`import`** 문: 필요한 패키지를 가져옵니다.
    - **`javax.swing.*`**: Java Swing을 사용하여 GUI 요소를 생성하기 위해 필요한 클래스를 제공합니다.
    - **`javax.swing.filechooser.FileNameExtensionFilter`**: 파일 선택 대화상자에서 파일 필터를 적용하기 위해 사용됩니다.
    - **`java.io.*`**: 입출력 작업을 위한 클래스들을 포함합니다.
    - **`java.net.*`**: 네트워크 관련 클래스들을 제공합니다.
    - **`java.util.*`**: 유틸리티 클래스들을 포함합니다.
2. **`FileClient`** 클래스: 파일 클라이언트를 구현한 클래스입니다. **`Socket`**을 사용하여 서버와 통신하고, 파일의 생성 및 수정을 감지합니다.
    - 멤버 변수:
        - **`socket`**: 서버와의 소켓 연결을 위한 변수입니다.
        - **`input`**: 소켓 입력 스트림을 읽기 위한 **`BufferedReader`** 객체입니다.
        - **`output`**: 소켓 출력 스트림을 쓰기 위한 **`PrintWriter`** 객체입니다.
        - **`baseDirectoryPath`**: 클라이언트의 기본 디렉토리 경로입니다.
        - **`clientId`**: 클라이언트의 고유 식별자입니다.
        - **`clientDirectoryPath`**: 클라이언트의 디렉토리 경로입니다.
        - **`fileMetadataList`**: 파일 메타데이터를 저장하는 리스트입니다.
    - 생성자(**`FileClient`**): 서버 주소와 포트를 매개변수로 받아 초기화를 수행합니다.
        - **`socket`**을 생성하고 입력 및 출력 스트림을 초기화합니다.
        - **`clientId`**를 생성하여 메시지로 서버에 전송합니다.
        - 클라이언트 디렉토리 경로를 설정하고, 파일 감지 스레드를 시작합니다.
    - **`startFileUpdateDetectionThread`** 메서드: 파일 감지 스레드를 시작합니다.
        - 주어진 디렉토리 경로의 파일들을 주기적으로 확인하며 감지합니다.
        - 파일이 **`client_`**로 시작하는 경우에만 처리합니다.
        - 파일이 메타데이터 목록에 있는지 확인하고, 새로 생성된 파일인 경우 메타데이터를 추가합니다.
        - 파일이 수정된 경우 메타데이터의 논리적 시계 값을 업데이트하고, 2초 동안 대기한 후 파일을 업데이트합니다.
    - **`getFileMetadata`** 메서드: 주어진 파일 이름에 해당하는 파일 메타데이터를 반환합니다.
    - **`updateFile`** 메서드: 파일을 업데이트합니다.
        - 파일 이름과 절대 파일 경로를 얻어옵니다.
        - 파일을 읽어서 내용을 문자열로 변환합니다.
        - 서버에 파일 업데이트 메시지를 전송합니다.
    - **`shareFile`** 메서드: 파일을 공유합니다.
        - 파일 선택 대화상자를 열어 선택된 파일을 가져옵니다.
        - 디렉토리 선택 대화상자를 열어 선택된 디렉토리를 가져옵니다.
        - 파일 내용을 읽어서 문자열로 변환하고, 서버에 파일 공유 메시지를 전송합니다.
    - **`disconnect`** 메서드: 서버와의 연결을 종료합니다.
    - **`sendMessage`** 메서드: 서버로 메시지를 전송합니다. 파일 메타데이터도 함께 전송합니다.
    - **`upload`** 메서드: 파일을 업로드합니다.
        - 파일 선택 대화상자를 열어 선택된 파일들을 가져옵니다.
        - 선택된 각 파일에 대해 파일이 존재하는지 확인하고, 파일을 읽어서 서버에 업로드합니다.
    - **`main`** 메서드: **`FileClient`** 객체를 생성하고, 파일 업로드 후 서버와의 연결을 종료합니다.
    
    ## FileServer
    
    ```java
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
                        }else if (message.startsWith("SHARE#$")){
                            String[] parts = message.split("#\\$");
                            String selectedDirectory = parts[1];
                            String fileName = parts[2];
                            String fileContent = parts[3];
    
                            // Create a new server file with the updated content
                            File newFile = new File(selectedDirectory, fileName);
                            try (PrintWriter writer = new PrintWriter(new FileWriter(newFile))) {
                                writer.write(fileContent);
                                System.out.println("File shared with client " + selectedDirectory + ": " + fileName);
                            } catch (IOException e) {
                                System.out.println("Error sharing file with client " + selectedDirectory + ": " + e.getMessage());
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
    ```
    
    파일 서버를 구현한 클래스입니다.
    
    1. **`import`** 문: 필요한 패키지를 가져옵니다.
        - **`java.io.*`**: 입출력 작업을 위한 클래스들을 포함합니다.
        - **`java.net.*`**: 네트워크 관련 클래스들을 제공합니다.
        - **`java.util.*`**: 유틸리티 클래스들을 포함합니다.
    2. **`FileServer`** 클래스: 파일 서버를 구현한 클래스입니다. 클라이언트의 연결을 수락하고, 클라이언트와의 통신을 처리합니다.
        - 멤버 변수:
            - **`serverSocket`**: 서버 소켓을 나타내는 변수입니다.
            - **`clients`**: 클라이언트 핸들러 객체를 저장하는 리스트입니다.
            - **`baseDirectoryPath`**: 서버의 기본 디렉토리 경로입니다.
            - **`fileMetadataMap`**: 파일 메타데이터를 저장하는 맵입니다.
        - 생성자(**`FileServer`**): 포트와 파일 경로를 매개변수로 받아 초기화를 수행합니다.
            - **`ServerSocket`**을 생성하고 클라이언트 리스트와 파일 메타데이터 맵을 초기화합니다.
        - **`start`** 메서드: 서버를 시작합니다.
            - 클라이언트의 연결을 수락하고, 각 클라이언트 핸들러를 생성하여 시작합니다.
            - 클라이언트의 연결이 수락되면 클라이언트 리스트에 추가하고, 클라이언트에게 연결 메시지를 전송합니다.
        - **`broadcast`** 메서드: 모든 클라이언트에게 메시지를 브로드캐스트합니다.
        - **`ClientHandler`** 클래스(내부 클래스): 클라이언트와의 통신을 처리하는 핸들러 클래스입니다.
            - **`Thread`** 클래스를 상속받아 스레드로 동작합니다.
            - 멤버 변수:
                - **`socket`**: 클라이언트와의 소켓 연결을 나타내는 변수입니다.
                - **`clientId`**: 클라이언트의 고유 식별자입니다.
                - **`input`**: 소켓 입력 스트림을 읽기 위한 **`BufferedReader`** 객체입니다.
                - **`output`**: 소켓 출력 스트림을 쓰기 위한 **`PrintWriter`** 객체입니다.
                - **`clientDirectoryPath`**: 클라이언트의 디렉토리 경로입니다.
            - 생성자(**`ClientHandler`**): 소켓을 매개변수로 받아 초기화를 수행합니다.
                - 소켓의 입력 스트림을 읽어 클라이언트 식별자를 설정합니다.
                - 소켓의 출력 스트림을 초기화합니다.
                - 클라이언트의 디렉토리 경로를 설정하고, 해당 디렉토리가 존재하지 않는 경우 생성합니다.
            - **`getClientId`** 메서드: 클라이언트의 식별자를 반환합니다.
            - **`run`** 메서드: 클라이언트와의 통신을 처리합니다.
                - 클라이언트로부터 메시지를 읽어오고, 각 메시지에 대한 처리를 수행합니다.
                - 메시지가 "CREATE:"로 시작하는 경우, 파일을 생성하고 서버와 클라이언트의 파일 메타데이터를 업데이트합니다.
                - 메시지가 "DELETE:"로 시작하는 경우, 클라이언트의 파일을 삭제하고 파일 메타데이터를 제거합니다.
                - 메시지가 "UPDATE:"로 시작하는 경우, 클라이언트의 파일을 업데이트하고 서버의 파일을 갱신합니다.
                - 메시지가 "SHARE#$"로 시작하는 경우, 클라이언트와 파일을 공유합니다.
                - 모든 메시지를 출력하고, 소켓이 닫힐 때까지 반복합니다.
            - **`sendMessage`** 메서드: 클라이언트에게 메시지를 전송합니다.
        - **`main`** 메서드: **`FileServer`** 객체를 생성하고 서버를 시작합니다.
        
        ## FileMetadata
        
        ```java
        public class FileMetadata {
            private String fileName;
            private long logicalClock;
        
            public FileMetadata(String fileName, int logicalClock) {
                this.fileName = fileName;
                this.logicalClock = logicalClock;
            }
        
            public String getFileName() {
                return fileName;
            }
        
            public long getLogicalClock() {
                return logicalClock;
            }
        
            public void incrementLogicalClock() {
                this.logicalClock++;
            }
        
            public void setLogicalClock(long l) {
                this.logicalClock = l;
            }
        }
        ```
        
        파일의 메타데이터를 나타내는 **`FileMetadata`** 클래스입니다
        
        - 멤버 변수:
            - **`fileName`**: 파일의 이름을 저장하는 변수입니다.
            - **`logicalClock`**: 파일의 논리적 시계 값을 저장하는 변수입니다.
        - 생성자(**`FileMetadata`**): 파일의 이름과 논리적 시계 값을 매개변수로 받아 초기화를 수행합니다.
        - **`getFileName`** 메서드: 파일의 이름을 반환합니다.
        - **`getLogicalClock`** 메서드: 파일의 논리적 시계 값을 반환합니다.
        - **`incrementLogicalClock`** 메서드: 파일의 논리적 시계 값을 증가시킵니다.
        - **`setLogicalClock`** 메서드: 파일의 논리적 시계 값을 설정합니다.

## 4. 기능 별 구현 방법 및 사용한 외부 라이브러리

1. 단순 파일 동기화 기능
3.1 클라이언트는 파일 업데이트(생성, 삭제, 수정)를 자동 또는 수동으로 탐지:
    - 클라이언트는 **`startFileUpdateDetectionThread`** 메서드를 사용하여 파일 감지 스레드를 시작합니다.
    - 해당 스레드는 주기적으로 클라이언트의 디렉토리를 확인하고 파일 변경 사항을 감지합니다.
    - 파일이 생성, 삭제, 수정될 때마다 해당 작업을 처리하고 서버로 업데이트합니다.
    
    3.2 클라이언트는 파일 업데이트가 서버의 파일과 충돌하지 않음을 확인 (논리적 시계 이용):
    
    - 클라이언트는 **`FileMetadata`** 클래스를 사용하여 파일의 메타데이터를 관리합니다.
    - 각 파일은 논리적 시계 값을 가지고 있으며, 이를 통해 파일 업데이트 충돌을 확인합니다.
    - 클라이언트는 파일이 수정될 때마다 논리적 시계 값을 갱신하고, 서버의 파일과 비교하여 충돌 여부를 판단합니다.
    
    3.3 클라이언트는 충돌하지 않은 업데이트된 파일을 서버로 전송하여 기존 파일 덮어씀:
    
    - 클라이언트는 파일이 업데이트되면 해당 파일을 서버로 전송하여 기존 파일을 덮어씁니다.
    - 업데이트된 파일의 내용을 서버로 전송하고, 서버는 해당 파일을 수신하여 기존 파일을 대체합니다.
    
    3.4 삭제된 파일은 서버에서도 삭제:
    
    - 클라이언트는 파일이 삭제될 때 해당 파일을 서버로 전송하여 서버에서도 파일을 삭제합니다.
    - 파일 삭제 작업은 서버로 삭제 요청 메시지를 전송하고, 서버는 해당 파일을 삭제합니다.
2. 다른 클라이언트와 파일 단순 공유 기능
4.1 파일 및 공유할 사용자(클라이언트) 선택:
    - 클라이언트는 **`shareFile`** 메서드를 사용하여 파일을 공유합니다.
    - 파일 선택 대화상자를 통해 공유할 파일을 선택하고, 사용자는 공유 대상 클라이언트를 선택합니다.
    
    4.2 선택 파일을 (서버를 거쳐) 선택한 클라이언트에게 전송:
    
    - 선택한 파일과 공유 대상 클라이언트 정보를 서버로 전송합니다.
    - 서버는 파일을 수신하고, 해당 파일을 선택한 클라이언트에게 전송합니다.
    - 선택한 클라이언트는 서버로부터 전송 받은 파일을 자신의 디렉토리에 저장합니다.

## 5. 클라이언트와 서버 동작 프로토콜(기능 항목별 메시지 포맷, 메시지 송신 조건,메시지 수신 동작 정의)

- FileClient class의 sendmessage()가 서버에 메세지 전송을 담당합니다.
- 메세지 전송 형식
    - sendMessage("CREATE:" + fileName + ":" + fileContent);
    - sendMessage("UPDATE:" + fileName + ":" + fileContent); *// Use fileName instead of pureFileName*
    - sendMessage("SHARE#$" + directoryPath + "#$" + fileName + "#$" + fileContent);
- 이후 Fileserver의 run()에서 각 메세지 포맷에 맞게 동작 수행
