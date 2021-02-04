package server;

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
    }

    // Вспомогательный метод для удаления клиента из списка клиентов
    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
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

    public void privateMsg(String msg, String nickname) {
        for (ClientHandler c : clients) {
            if (c.getNickname().equalsIgnoreCase(nickname)) {
                c.sendMsg(msg);
            }
        }
    }
}
