package server;

import commands.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;

    private DataInputStream in;
    private DataOutputStream out;

    private String nickname;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            // Считываем входящий канал у клиентского socket
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(()-> {
                try {
                    // Цикл аутентификации
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")){ // Служебные сообщения с символом /
                            if (str.equals(Command.END)) { // Команда для прерывания пользователем переписки
                                System.out.println("Пользователь хочет отключиться от сервера ");
                                out.writeUTF(Command.END); // Команда для отключения пользователя при идентификации
                                throw new RuntimeException("Пользователь хочет отключиться от сервера");
                            }
                            // Сообщения о состоянии аутентификации
                            if (str.startsWith(Command.AUTH)) { // Успешная авторизация
                                String[] token = str.split("\\s");
                                String newNick = server.getAuthService().getNicknameByLoginAndPassword(token[1], token[2]);
                                if (newNick != null){ // Успешная утентификация
                                    nickname = newNick;
                                    sendMsg(Command.AUT_OK + " " + nickname); // Отправка сообщения при успешной авторизации
                                    server.subscribe(this);
                                    break;
                                } else {
                                    sendMsg("Неверный логин / пароль");

                                }
                            }

                        }
                    }
                    // Цикл работающий после аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.equals(Command.END)) { // Команда для прерывания пользователем переписки
                            out.writeUTF(Command.END);
                            break;

                            // Реализуем личные сообщения от пользователя к пользователю
                        }
                        if (str.startsWith(Command.W)) {
                            String[] words = str.split("\\s", 2);
                            String nickReciever = words[1];
                            String message = str.substring(3);
                            server.privateMsg(" [ " + nickname + " ] " + " : " + message, nickReciever);

                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("Клиент отключился");
                    server.unsubscribe(this); // Удалим отключившегося клиента из списка клиентов
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start(); // Запуск потока

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Для отправки сообщения от сервера к конкретному клиенту
    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }
}
