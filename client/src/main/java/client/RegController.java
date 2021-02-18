package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class RegController {
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField nicknameField;
    @FXML
    private TextArea textArea;

    private Controller controller;

    public void setController(Controller controller) {
        this.controller = controller;
    }

    public void tryToReg(ActionEvent actionEvent) {
        String login = loginField.getText().trim(); // trim - удаляет лишние пробелы
        String password = passwordField.getText().trim();
        String nickname = nicknameField.getText().trim();

        // Проверка корректности заполнения полей регистрации
        if (login.length()*password.length()*nickname.length() == 0) {
            textArea.appendText("Логин, пароль и никнейм не должны быть пустуми\n");
            return;
        }
        if (login.contains(" ") || password.contains(" ") || nickname.contains(" ")) {
            textArea.appendText("Логин, пароль и никнейм не должны содержать пробелы\n");
            return;
        }

        controller.tryToReg(login, password, nickname);

    }

    // Метод реагирующий на действие регистрации пользователем
    public void resultTryToReg(boolean flag) {
        if(flag) {
            textArea.appendText("Регистрация прошла успешно\n");
        } else {
            textArea.appendText("Регистрация не удалась.\n" +
                    "Возможно логин или никнейм используются\n другим пользователем.\n");
        }
    }
}
