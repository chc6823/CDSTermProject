import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileTransferServer {
    private static final int PORT_NUMBER = 4444;
    private static final String FILE_PATH = "C:\\Users\\chc68\\OneDrive\\바탕 화면\\server";

    private final ByteBuffer buffer = ByteBuffer.allocate(1024);

    public void startServer() throws IOException {
        // 서버 소켓 채널 열기
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(PORT_NUMBER));
        serverSocketChannel.configureBlocking(false);

        // selector 열기
        Selector selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        // 클라이언트 스레드 풀 생성
        ExecutorService clientThreadPool = Executors.newFixedThreadPool(5);

        while (true) {
            int readyChannels = selector.select();
            if (readyChannels == 0) {
                continue;
            }

            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                if (key.isAcceptable()) {
                    // 새로운 클라이언트 요청
                    SocketChannel clientChannel = serverSocketChannel.accept();
                    clientChannel.configureBlocking(false);
                    clientChannel.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    // 클라이언트 요청 처리
                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    buffer.clear();
                    int bytesRead = clientChannel.read(buffer);

                    if (bytesRead > 0) {
                        // 파일 저장
                        String content = new String(buffer.array(), 0, bytesRead);
                        String fileName = "Server - " + System.currentTimeMillis() + ".txt";
                        File file = new File(FILE_PATH + File.separator + fileName);
                        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                            writer.print(content);
                            writer.flush();
                        }

                        // 파일 전송 완료 메시지 전송
                        ByteBuffer responseBuffer = ByteBuffer.wrap("File transfer completed.".getBytes());
                        clientChannel.write(responseBuffer);
                    } else {
                        // 클라이언트 접속 종료
                        key.cancel();
                        clientChannel.close();
                    }
                }

                keyIterator.remove();
            }
        }
    }
}
