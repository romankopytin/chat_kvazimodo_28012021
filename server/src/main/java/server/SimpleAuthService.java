package server;

import java.util.ArrayList;
import java.util.List;

public class SimpleAuthService implements AuthService{

    private class UseData { // Класс для данных пользователей
        String login;
        String password;
        String nickname;

        public UseData(String login, String password, String nickname) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
        }
    }

    private List<UseData> users;

    // Создадим пользователей
    public SimpleAuthService() {
        users = new ArrayList<>();
        users.add(new UseData("qwe", "qwe", "qwe"));
        users.add(new UseData("asd", "asd", "asd"));
        users.add(new UseData("zxc", "zxc", "zxc"));
        for (int i = 0; i < 10; i++) {
            users.add(new UseData("login" + i, "pass" + i, "nick" + i)); // Дополнительные учетки пользователей до 9
        }
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        for (UseData user : users) { // Проверка наличия учетных записей
            if(user.login.equals(login) && user.password.equals(password)){ // Проверка наличия логина и пароля
                return user.nickname; // Если пароль определен, то возвращаем никнейм пользователя
            }
        }
        return null;
    }
}
