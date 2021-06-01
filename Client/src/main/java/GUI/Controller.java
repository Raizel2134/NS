package GUI;

import Client.Client;
import Message.CommandMessage;
import Message.DataTransferMessage;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    private Path ROOT = Paths.get(".").toAbsolutePath();
    private static final int MAX_OBJ_SIZE = 1024 * 1024 * 100;

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
    @FXML
    private ComboBox<String> disksBox;
    @FXML
    private TextField pathField;

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
        authPanel.setVisible(!clientConnection.isAuthorized());
        workArea.setVisible(clientConnection.isAuthorized());

        actionPanel1.setVisible(clientConnection.isAuthorized());
        actionPanel1.setManaged(clientConnection.isAuthorized());

        actionPanel2.setVisible(clientConnection.isAuthorized());
        actionPanel2.setManaged(clientConnection.isAuthorized());
    }

    public void showMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message);
    }

    private String showInputDialog() {
        return JOptionPane.showInputDialog(null, "Введите новое имя");
    }

    public void downloadFileToDisk() {
        File file = cloudList.getSelectionModel().getSelectedItem();
        clientConnection.sendMessage(new CommandMessage(CommandMessage.Command.DOWNLOAD, file));
    }

    public void uploadToServer() {
        File file = localList.getSelectionModel().getSelectedItem();
        if (file.length() >= MAX_OBJ_SIZE){
            showMessage(null, "Размер файла слишком большой!\nРекомендуемый размер файла менее 100мб");
        } else {
            clientConnection.sendMessage(new DataTransferMessage(Paths.get(file.getAbsolutePath())));
        }
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
                localList.sort();
            } else {
                Files.createDirectory(ROOT);
                localListItems.clear();
                localListItems.addAll(Files.list(ROOT).map(Path::toFile).collect(Collectors.toList()));
                localList.sort();
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

        pathField.setText(String.valueOf(ROOT));
        TableColumn<File, String> tcName = new TableColumn<>("Имя Файла");
        tcName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
        tcName.setPrefWidth(200);

        TableColumn<File, String> tcType = new TableColumn<>("Тип файла");
        tcType.setPrefWidth(140);
        tcType.setCellValueFactory(param -> {
            String type = param.getValue().getName();
            int index = type.indexOf('.');
            return new ReadOnlyObjectWrapper<>(index == -1 ? "" : "Файл " + type.substring(index));
        });
        TableColumn<File, String> tcSize = new TableColumn<>("Размер");
        tcSize.setCellValueFactory(param -> {
            long size = param.getValue().length();
            String type = param.getValue().getName();
            int index = type.indexOf('.');
            return new ReadOnlyObjectWrapper<>((index == -1 ? "[DIR]" : String.format("%,d bytes", size)));
        });
        tcSize.setPrefWidth(200);

        localList.getColumns().addAll(tcName, tcType, tcSize);
        localList.setItems(localListItems);
        localList.getSortOrder().add(tcName);
        cloudList.setItems(cloudListItems);

        disksBox.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            disksBox.getItems().add(p.toString());
        }
        disksBox.getSelectionModel().select(0);

        localList.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    Path path = ROOT.resolve(localList.getSelectionModel().getSelectedItem().getName());
                    if (Files.isDirectory(path)) {
                        updateList(path);
                    }
                }
            }
        });
        refreshListClient();
    }

    public ObservableList<File> getCloudListItems() {
        return cloudListItems;
    }

    public Path getROOT() {
        return ROOT;
    }

    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        updateList(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public void btnPathUpAction() {
        Path upperPath = ROOT.getParent();
        if (upperPath != null) {
            updateList(upperPath);
        }
    }

    private void updateList(Path path) {
        try {
            pathField.setText(String.valueOf(path));
            localListItems.clear();
            localListItems.addAll(Files.list(path).map(Path::toFile).collect(Collectors.toList()));
            localList.sort();
            this.ROOT = path;
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "По какой-то причине не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }
}
