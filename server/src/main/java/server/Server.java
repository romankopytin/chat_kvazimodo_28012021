package server;

import commands.Command;

import javax.swing.*;
import javax.swing.text.rtf.RTFEditorKit;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private ServerSocket server;
    private Socket socket;
    private final int PORT = 8189;
    private List<ClientHandler> clients;
    private AuthService authService;

    public Server() { // Конструктор сервера
        clients = new CopyOnWriteArrayList<>(); // Сделаем копию листа массива для работы со списком клиентов в несколько потоков
        authService = new SimpleAuthService(); // Инициализируем сервис авторизации пользователей
        try {
            // Инициализировали сервер
            server = new ServerSocket(PORT);
            System.out.println("Сервер запущен");

            // Сервер слушает выделенный ему порт
            while (true) { // Для ожидания клиентов
                socket = server.accept();
                System.out.println("Клиент подключился" + socket.getRemoteSocketAddress());
                new ClientHandler(this, socket); // Добавим клиентов в список
            }

            // Обработка ошибок связи (если занят порт)
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Вспомогательный метод для определения подключенных клиентов
    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastClientList(); // Изменение списка пользователй при входе новых пользователей
    }

    // Вспомогательный метод для удаления клиента из списка клиентов
    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList(); // Изменение списка пользователей при выходе пользователя
    }

    public AuthService getAuthService() {
        return authService;
    }

    // Метод для отправки сообщения всем клиентам из клиентского листа clients
    public void broadcastMsg(ClientHandler sender, String msg) {
        String message = String.format("[ %s ] : %s", sender.getNickname(), msg);
        for (ClientHandler c : clients) { // Пройдемся по всем клиентам
            c.sendMsg(message); // Отправим всем подключенным клиентам копию сообщения

        }
    }

    public void privateMsg(ClientHandler sender, String receiver, String msg) { // Укажем отправителя, получателя и сообщение передающееся конкретному пользователяю
        String message = String.format("[ %s ] => [ %s ] : %s", sender.getNickname(), receiver, msg);
        for (ClientHandler c : clients) { // Пройдемся по всем клиентам
            if (c.getNickname().equals(receiver)) { // Имя адресата = имени получателя - отправляем сообщение
                c.sendMsg(message); // Отправим подключенному клиенту копию сообщения
                if (!c.equals(sender)) { // Если адресат = пользователь отправляющий сообщение - не делаем копию сообщения для клиента пользователя
                    sender.sendMsg(String.format("=>" + " [ " + receiver + " ] : " + msg));
                }
                return;
            }
        }
        sender.sendMsg("Пользователь не найден: " + receiver); // Сообщение, если получателя сообщения нет в списке пользователей
    }

    // Сделаем так, чтобы один пользователь не мог зайти несколько раз в аккаунт
    public boolean isLoginAuthenticated(String login) {
        for (ClientHandler c : clients) {
            if(c.getLogin().equals(login)) {
                return true; // Пользователь авторизовался
            }
        }
        return false; // Пользователь не авторизовался
    }

    public void broadcastClientList() {
        StringBuilder sb = new StringBuilder(Command.CLIENT_LIST); // Соберем список имен
        for (ClientHandler c : clients) {
            sb.append(" ").append(c.getNickname());
        }
        // Передадим список пользователей в чат
        String message = sb.toString();
        for (ClientHandler c : clients) {
            c.sendMsg(message);
        }
    }
}
