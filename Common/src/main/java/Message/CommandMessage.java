package Message;

import java.io.File;

public class CommandMessage extends Message {
    public enum Command {
        DELETE,
        DOWNLOAD,
        LIST_FILES,
        RENAME
    }

    private Command command;
    private String addition;
    private File additionFile;

    public Command getCommand() {
        return command;
    }

    public CommandMessage(Command command) {
        this.command = command;
    }

    public CommandMessage(Command command, String addition) {
        this.command = command;
        this.addition = addition;
    }

    public CommandMessage(Command command, File additionFile) {
        this.command = command;
        this.additionFile = additionFile;
    }

    public CommandMessage(Command command, File additionFile, String newName) {
        this.command = command;
        this.additionFile = additionFile;
        this.addition = newName;
    }

    public String getAddition() {
        return addition;
    }

    public File getAdditionFile() {
        return additionFile;
    }
}
