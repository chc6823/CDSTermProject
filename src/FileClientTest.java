public class FileClientTest {
    private static final int NUM_CLIENTS = 3;

    public static void main(String[] args) {
        for (int i = 0; i < NUM_CLIENTS; i++) {
            // 클라이언트 생성 및 서버 접속
            FileClient client = new FileClient("localhost", 12345);
            client.upload();

            // 파일 업로드 또는 다운로드
            client.upload();
            // client.download("filename.txt");

            // 서버와의 연결 해제
            client.disconnect();
        }
    }
}
