package client;

import commands.Command;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    private TextArea textArea;
    @FXML
    private TextField textField;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private HBox authPanel;
    @FXML
    private HBox messagePanel;

    private Socket socket;
    private final int PORT = 8189;
    private final String IP_ADDRESS = "localhost";

    private DataInputStream in;
    private DataOutputStream out;

    // Введем переменную для изменения вида окна с авторизацией и без авторизации
    private boolean authenticated;
    private String nickname;

    private Stage stage; // Область окна для отображения теста

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        // Покажем текстовое окно при успешной авторизации
        messagePanel.setVisible(authenticated);
        messagePanel.setManaged(authenticated);
        // Скроем текстовое окно при неуспешной авторизации
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);

        // Если выходим из учетки, поле nickname очищается
        if (!authenticated) {
            nickname = "";
        }

        setTitle(nickname);
        textArea.clear(); // Очистим сообщения пользователя при разлогировании
    }

    @Override
    public void initialize(URL location, ResourceBundle resources){
        Platform.runLater(()->{
            stage = (Stage) textField.getScene().getWindow();
        });

        setAuthenticated(false);
    }

    // Метод для авторизации и разлогирования
    private void connect(){
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(()->{ // Поток для ожидания сообщения от сервера
                try {
                    // Цикл аутентификации
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")){ // Служебные сообщения с символом /
                            if (str.equals(Command.END)) { // Команда для прерывания пользователем переписки
                                System.out.println("Клиент отключился");
                                throw  new RuntimeException("Сервер отключил пользователя");
                            }
                            // Сообщения о состоянии аутентификации
                            if (str.startsWith(Command.AUT_OK)) { // Успешная авторизация
                                nickname = str.split("\\s")[1]; // Чтобы запомнить никнейм, разобьем его. \\s - используется для определения пробела и может писаться как " ".
                                setAuthenticated(true); // Успешная попытка аутентификации
                                break;
                            }
                        } else {
                            textArea.appendText(str + "\n");
                        }
                    }
                    // Цикл работающий до аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.equals(Command.END)) { // Команда для прерывания пользователем переписки
                            setAuthenticated(false);
                            break;
                        }

                        textArea.appendText(str + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    setAuthenticated(false);
                    try {
                        socket.close();
                    } catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(ActionEvent actionEvent) {
        try {
            if (textField.getText().trim().length() > 0) { // Чтобы не отправлялись сообщения с пробелами или пустые сообщения
                out.writeUTF(textField.getText()); // Для передачи текста на сервер
                textField.clear();
                textField.requestFocus();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void trytoAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) { // Соединение с сервером
            connect();
        }

        String msg = String.format("%s %s %s", Command.AUTH, loginField.getText().trim(), passwordField.getText().trim()); // Сообщение на сервер для авторизации пользователя
        try {
            out.writeUTF(msg);
            passwordField.clear(); // Очистим поле пароля после попытки авторизации
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод для вывода никнейма в заголовок чата
    private void setTitle(String title){
        Platform.runLater(()-> {
            if(title.equals("")){
                stage.setTitle("Сетевой чат - Квазимодо");
            } else {
                stage.setTitle("Сетевой чат - Квазимодо [" + title + "]");
            }
        });
    }
}
