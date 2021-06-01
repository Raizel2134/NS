package Message;

import java.nio.file.Files;
import java.nio.file.Path;

public class DataTransferMessage extends Message {
    private String fileName;
    private String path;
    private byte[] data;
    private int size;

    public DataTransferMessage(Path path) {
        this.path = path.toString();
        this.fileName = path.getFileName().toString();
        try {
            this.data = Files.readAllBytes(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.size = data.length;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getData() {
        return data;
    }
}
