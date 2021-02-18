package client;

import commands.Command;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.w3c.dom.ls.LSOutput;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    public ListView<String> clientList;
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
    private Stage regStage; // Окно с формой регистрации
    private RegController regController;

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        // Покажем текстовое окно при успешной авторизации
        messagePanel.setVisible(authenticated);
        messagePanel.setManaged(authenticated);
        // Скроем текстовое окно при неуспешной авторизации
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        clientList.setVisible(authenticated);
        clientList.setManaged(authenticated);

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
            // Событие при закрытии окна "х"
            stage.setOnCloseRequest(event -> {
                System.out.println("Чат закрыт");
                if (socket != null && socket.isClosed()) { // Если сокет не инициализирован (null) или закрыт
                    try {
                        out.writeUTF(Command.END); // Используем команду разрыва соединения
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
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
                        if (str.startsWith("/")) { // Служебные сообщения с символом /
                            if (str.equals(Command.END)) { // Команда для прерывания пользователем переписки
                                System.out.println("Клиент отключился");
                                throw new RuntimeException("Сервер отключил пользователя");
                            }
                            // Сообщения о состоянии аутентификации
                            if (str.startsWith(Command.AUT_OK)) { // Успешная авторизация
                                nickname = str.split("\\s")[1]; // Чтобы запомнить никнейм, разобьем его. \\s - используется для определения пробела и может писаться как " ".
                                setAuthenticated(true); // Успешная попытка аутентификации
                                break;
                            }
                            // Комманды при попытке пользователя зарегистрироваться
                            if(str.equals(Command.REG_OK)) {
                                regController.resultTryToReg(true);
                            }
                            if(str.equals(Command.REG_NO)) {
                                regController.resultTryToReg(false);
                            }
                        } else {
                            textArea.appendText(str + "\n");
                        }
                    }
                    // Цикл работы чата после аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) { // Определим служебные сообщения
                            if (str.equals(Command.END)) { // Команда для прерывания пользователем переписки
                                setAuthenticated(false);
                                break;
                            }
                            // Получим список ползователей
                            if (str.startsWith(Command.CLIENT_LIST)) {
                                String[] token = str.split("\\s");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear(); // Очистим список пользователей
                                    for (int i = 1; i < token.length; i++) {
                                        clientList.getItems().add(token[i]); // Добавим пользователей
                                    }
                                });
                            }
                        } else {
                            textArea.appendText(str + "\n");
                        }
                    }
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
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

    public void clientListClicked(MouseEvent mouseEvent) {
        System.out.println(clientList.getSelectionModel().getSelectedItem());
        String msg = String.format("%s %s ", Command.PRIVATE_MSG, clientList.getSelectionModel().getSelectedItem());
        textField.setText(msg);
    }

    // Кнопка вызова окна регистрации
    public void clickRegButton(ActionEvent actionEvent) {
        if(regStage == null) { // Если окна регистрации нет, то кнопка его вызовет
            createRegWindow();
        }
        regStage.show(); // Показать окно рагистрации
    }

    private void createRegWindow(){
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();

            regController = fxmlLoader.getController(); // Инициализируем окном регистрации
            regController.setController(this);

            regStage = new Stage();
            regStage.setTitle("Сетевой чат - Квазимодо - Регистрация");
            regStage.setScene(new Scene(root, 400, 300));

            // Сделаем окно регистрации на переднем плане приложения
            regStage.initModality(Modality.APPLICATION_MODAL); // Пока окно регитсрации не закрыто, основное окно приложение будет недоступно
            regStage.initStyle(StageStyle.UTILITY); // Отключим возможноть сворачивания окна

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Вызовем из контроллера метод и передадим ему параметры выше
    public void tryToReg (String login, String password, String nickname) {
        String message = String.format("%s %s %s %s", Command.REG, login, password, nickname); // Передадим из окна регистрации данные - комманда регистрации, логин, пароль и никмейн
        if (socket == null || socket.isClosed()) { // Соединение с сервером - проверка открыт ли сокет
            connect();
        }
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
