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
