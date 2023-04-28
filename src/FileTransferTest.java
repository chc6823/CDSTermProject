// FileTransferTest.java

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileTransferTest {
    private static final int NUM_CLIENTS = 4;

    public static void main(String[] args) {
        // 클라이언트 스레드 풀 생성
        ExecutorService clientThreadPool = Executors.newFixedThreadPool(NUM_CLIENTS);

        // 여러 개의 클라이언트가 동시에 서버에 접속하고 파일 전송을 시도하도록 함
        for (int i = 1; i < NUM_CLIENTS; i++) {
            Runnable client = new Runnable() {
                @Override
                public void run() {
                    try {
                        FileTransferClient.main(null);
                    } catch (IOException e) {
                        System.out.println("Error creating client: " + e);
                    }
                }
            };
            clientThreadPool.execute(client);
        }
        clientThreadPool.shutdown();
    }
}

