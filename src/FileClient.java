import java.io.*;
import java.net.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileClient {
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    public FileClient(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Connected to server " + serverAddress + ":" + serverPort);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
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
    }

    public void upload() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            for (File file : files) {
                if (!file.exists()) {
                    System.out.println("Error: File not found");
                    return;
                }
                try {
                    FileInputStream fileInput = new FileInputStream(file);
                    ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fileInput.read(buffer)) != -1) {
                        byteArrayOutput.write(buffer, 0, bytesRead);
                    }
                    fileInput.close();
                    byteArrayOutput.flush();
                    byte[] bytes = byteArrayOutput.toByteArray();
                    String fileName = file.getName();
                    String fileContent = new String(bytes);
                    sendMessage("FILE:" + fileName + ":" + fileContent);
                    System.out.println("File uploaded successfully: " + fileName);
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        FileClient client = new FileClient("localhost", 12345);
        client.upload();
        client.disconnect();
    }
}
