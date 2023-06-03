import java.io.*;
import java.util.*;

//10mb짜리 대용량 파일 생성
public class LargeFileCreator {
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static void main(String[] args) {
        Random rand = new Random();
        try {
            PrintWriter writer = new PrintWriter(new FileWriter("largeFile.txt"));
            long fileSize = 0;
            while (fileSize < 10 * 1024 * 1024) { // 10 MB
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 1024; i++) { // Create 1 KB of text
                    char ch = CHARACTERS.charAt(rand.nextInt(CHARACTERS.length()));
                    sb.append(ch);
                }
                writer.println(sb.toString());
                fileSize += 1024;
            }
            writer.close();
            System.out.println("Created a 10MB file named largeFile.txt");
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
