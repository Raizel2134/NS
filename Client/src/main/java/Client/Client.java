package Client;


import GUI.Controller;
import Message.*;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Client {
    private static final String HOST = "localhost";
    private static final int PORT = 8189;
    private Socket socket;
    private ObjectEncoderOutputStream oeos;
    private ObjectDecoderInputStream odis;
    private boolean isAuthorized;
    private String username;
    private boolean isConnected;

    public boolean isAuthorized() {
        return isAuthorized;
    }

    public void setAuthorized(boolean authorized) {
        isAuthorized = authorized;
    }

    public Client() {
    }

    public void init(Controller controller) {
        try {
            this.socket = new Socket(HOST, PORT);
            this.oeos = new ObjectEncoderOutputStream(socket.getOutputStream());
            this.odis = new ObjectDecoderInputStream(socket.getInputStream());
            this.isAuthorized = false;
            this.isConnected = true;

            new Thread(() -> {
                try {
                    while (true) {
                        Object message = odis.readObject();
                        if (message != null) {
                            if (message instanceof ResultMessage.Result) {
                                if (message == ResultMessage.Result.OK) {
                                    setAuthorized(true);
                                    controller.switchWindows();
                                    controller.showMessage(null, "Добро пожаловать");
                                    break;
                                } else {
                                    controller.showMessage(null, "Логин или пароль не верен, пожалуйста повторите");
                                    break;
                                }
                            }
                        }
                    }
                    while (isConnected) {
                        Object message = odis.readObject();
                        if (message != null) {
                            if (message instanceof FileListMessage) {
                                FileListMessage fm = (FileListMessage) message;
                                Platform.runLater(() -> {
                                    controller.getCloudListItems().clear();
                                    controller.getCloudListItems().addAll(fm.getFiles());
                                });
                            }
                            if (message instanceof DataTransferMessage) {
                                DataTransferMessage dataTransferMessage = (DataTransferMessage) message;
                                Path path = Paths.get(controller.getROOT() + "\\" + dataTransferMessage.getFileName());
                                try {
                                    if (Files.exists(path)) {
                                        Files.write(path, dataTransferMessage.getData(), StandardOpenOption.TRUNCATE_EXISTING);
                                    } else {
                                        Files.write(path, dataTransferMessage.getData(), StandardOpenOption.CREATE);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                controller.refreshListClient();
                            }
                        }
                    }
                } catch (IOException e) {
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    disconnect();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Message message) {
        try {
            oeos.writeObject(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void auth(String login, String password) {
        try {
            AuthMessage authMessage = new AuthMessage(login, password);
            this.username = login;
            oeos.writeObject(authMessage);
            oeos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            odis.close();
            oeos.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
