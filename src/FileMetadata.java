public class FileMetadata {
    private String fileName;
    private int logicalClock;

    public FileMetadata(String fileName, int logicalClock) {
        this.fileName = fileName;
        this.logicalClock = logicalClock;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLogicalClock() {
        return logicalClock;
    }

    public void incrementLogicalClock() {
        this.logicalClock++;
    }
}
