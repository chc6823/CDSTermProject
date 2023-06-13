class FileClientTest {
    private static final int NUM_CLIENTS = 3;

    public static void main(String[] args) {
        for (int i = 0; i < NUM_CLIENTS; i++) {
            FileClient client = new FileClient("localhost", 12345);
            client.upload();
            client.disconnect();
        }
    }
}