import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;

public class FileTransferTest {
    private static final int PORT_NUMBER = 4444;
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) throws IOException {
        // Selector 생성
        Selector selector = Selector.open();

        // ServerSocketChannel 생성
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(PORT_NUMBER));

        // ServerSocketChannel을 Selector에 등록
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server started on port " + PORT_NUMBER);

        // Selector를 사용하여 이벤트를 처리하는 무한루프
        while (true) {
            int selectedCount = selector.select();
            if (selectedCount == 0) {
                continue;
            }

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();

                if (key.isAcceptable()) { // 연결 요청에 대한 이벤트 처리
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel client = server.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ);
                    System.out.println("New client connected: " + client.getRemoteAddress());

                } else if (key.isReadable()) { // 데이터 수신에 대한 이벤트 처리
                    SocketChannel client = (SocketChannel) key.channel();

                    // 수신한 데이터를 ByteBuffer에 저장
                    ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                    int bytesRead = client.read(buffer);
                    if (bytesRead == -1) {
                        System.out.println("Client disconnected: " + client.getRemoteAddress());
                        client.close();
                        continue;
                    }
                    buffer.flip();

                    // ByteBuffer에 저장된 데이터를 String으로 변환하여 출력
                    String data = Charset.defaultCharset().decode(buffer).toString();
                    System.out.println("Received from client " + client.getRemoteAddress() + ": " + data);

                    // 데이터 송신
                    client.write(buffer);

                } else if (key.isWritable()) { // 데이터 송신에 대한 이벤트 처리
                    // TODO: 데이터 송신 처리
                }

                iterator.remove(); // 현재 처리한 이벤트를 제거하여 다음 이벤트 처리를 위해 Selector에 등록된 키 목록에서 제거
            }
        }
    }
}
