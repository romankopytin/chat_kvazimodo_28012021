package server;

public interface AuthService {
    String getNicknameByLoginAndPassword (String login, String password); // Метод принимает логин и пароль, в ответ возвращает никнейм
}
