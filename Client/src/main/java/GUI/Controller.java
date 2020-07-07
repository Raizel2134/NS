package GUI;

import Client.Client;
import Message.CommandMessage;
import Message.DataTransferMessage;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    private final Path ROOT = Paths.get("./");

    @FXML
    private TableView<File> localList;
    @FXML
    private ListView<File> cloudList;
    @FXML
    private Pane authPanel;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passField;
    @FXML
    private Pane workArea;
    @FXML
    private HBox actionPanel1;
    @FXML
    private HBox actionPanel2;

    private Client clientConnection;

    private ObservableList<File> cloudListItems;
    private ObservableList<File> localListItems;

    @FXML
    public void auth() {
        clientConnection.auth(loginField.getText(), passField.getText());
        loginField.clear();
        passField.clear();
    }

    public void switchWindows() {
        authPanel.setVisible(!clientConnection.isAuthrozied());
        workArea.setVisible(clientConnection.isAuthrozied());

        actionPanel1.setVisible(clientConnection.isAuthrozied());
        actionPanel1.setManaged(clientConnection.isAuthrozied());

        actionPanel2.setVisible(clientConnection.isAuthrozied());
        actionPanel2.setManaged(clientConnection.isAuthrozied());
    }

    public void showMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message);
    }

    public String showInputDialog() {
        return JOptionPane.showInputDialog(null, "Введите новое имя");
    }

    public void downloadFileToDisk() {
        File file = cloudList.getSelectionModel().getSelectedItem();
        clientConnection.sendMessage(new CommandMessage(CommandMessage.Command.DOWNLOAD, file));
    }

    public void uploadToServer() {
        File file = localList.getSelectionModel().getSelectedItem();
        clientConnection.sendMessage(new DataTransferMessage(Paths.get(file.getAbsolutePath())));
    }

    public void deleteFromLocalDisk() {
        try {
            Files.delete(Paths.get(localList.getSelectionModel().getSelectedItem().getAbsolutePath()));
            refreshListClient();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteFromServer() {
        clientConnection.sendMessage(new CommandMessage(CommandMessage.Command.DELETE, cloudList.getSelectionModel().getSelectedItem().getAbsolutePath()));
    }

    public void refreshListClient() {
        try {
            if (Files.exists(ROOT)) {
                localListItems.clear();
                localListItems.addAll(Files.list(ROOT).map(Path::toFile).collect(Collectors.toList()));
            } else {
                Files.createDirectory(ROOT);
                localListItems.clear();
                localListItems.addAll(Files.list(ROOT).map(Path::toFile).collect(Collectors.toList()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void refreshListServer() {
        clientConnection.sendMessage(new CommandMessage(CommandMessage.Command.LIST_FILES));
    }

    public void renameFileOnServer() {
        String newName = showInputDialog();
        clientConnection.sendMessage(new CommandMessage(CommandMessage.Command.RENAME, cloudList.getSelectionModel().getSelectedItem(), newName));
    }

    public void renameLocalFile() {
        String newName = showInputDialog();
        String expansion = null;
        try {
            expansion = localList.getSelectionModel().getSelectedItem().getName().split("\\.")[1];
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Path source = Paths.get(localList.getSelectionModel().getSelectedItem().getPath());
            if (expansion == null) {
                Files.move(source, source.resolveSibling(newName));
            } else {
                if (newName.contains(".")) {
                    Files.move(source, source.resolveSibling(newName));
                } else {
                    Files.move(source, source.resolveSibling(newName + "." + expansion));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        refreshListClient();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.clientConnection = new Client();
        clientConnection.init(this);
        cloudListItems = FXCollections.observableArrayList();
        localListItems = FXCollections.observableArrayList();

        TableColumn<File, String> tcName = new TableColumn<>("Имя Файла");
        tcName.setCellValueFactory(new PropertyValueFactory<File,String>("name"));
        tcName.setPrefWidth(200);
        TableColumn<File,String> tcSize = new TableColumn<>("Размер");
        tcSize.setCellValueFactory(param -> {
            long size = param.getValue().length();
            return new ReadOnlyObjectWrapper<>(String.valueOf(size) + " bytes");
        });
        tcSize.setPrefWidth(200);
        localList.getColumns().addAll(tcName, tcSize);

        localList.setItems(localListItems);
        cloudList.setItems(cloudListItems);

        refreshListClient();
    }

    public ObservableList<File> getCloudListItems() {
        return cloudListItems;
    }

    public Path getROOT() {
        return ROOT;
    }
}
