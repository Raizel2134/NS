package Message;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class FileListMessage extends Message {
    private List<File> files;

    public List<File> getFiles() {
        return files;
    }

    public FileListMessage(Path path) {
        try {
            files = Files.list(path).map(Path::toFile).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
