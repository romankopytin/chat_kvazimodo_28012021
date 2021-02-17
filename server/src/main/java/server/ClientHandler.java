package server;

import commands.Command;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientHandler {
    private Server server;
    private Socket socket;

    private DataInputStream in;
    private DataOutputStream out;

    private String nickname;
    private String login;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            // Считываем входящий канал у клиентского socket
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    socket.setSoTimeout(5000); // Если пользователь бездействует 120 сек перед авторизацией

                    // Цикл аутентификации
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) { // Служебные сообщения с символом /
                            if (str.equals(Command.END)) { // Команда для прерывания пользователем переписки
                                System.out.println("Пользователь хочет отключиться от сервера ");
                                out.writeUTF(Command.END); // Команда для отключения пользователя при идентификации
                                throw new RuntimeException("Пользователь хочет отключиться от сервера");
                            }
                            // Сообщения о состоянии аутентификации
                            if (str.startsWith(Command.AUTH)) { // Успешная авторизация
                                String[] token = str.split("\\s");
                                String newNick = server.getAuthService().getNicknameByLoginAndPassword(token[1], token[2]);
                                login = token[1]; // Определяем логин
                                if (newNick != null) { // Успешная утентификация
                                    socket.setSoTimeout(0); // Отменим отключение при бездействии
                                    if (!server.isLoginAuthenticated(login)) { // Если пользователь не авторизовался
                                        nickname = newNick;
                                        sendMsg(Command.AUT_OK + " " + nickname); // Отправка сообщения при успешной авторизации
                                        server.subscribe(this);
                                        break;
                                    } else {
                                        sendMsg("Пользователь с такой учетной записью уже авторизовался");
                                    }
                                } else {
                                    sendMsg("Неверный логин / пароль");
                                }
                            }
                            if (str.startsWith(Command.REG)) {
                                String[] token = str.split("\\s");
                                if (token.length < 4) { // Если пришло меньше 4-х сущностей - комманда, логин, пароль и никнейм, то не даем зарегистрироваться
                                    continue;
                                }
                                boolean regSuccessful = server.getAuthService().registration(token[1], token[2], token[3]);
                                if (regSuccessful) {
                                    sendMsg(Command.REG_OK);
                                } else {
                                    sendMsg(Command.REG_NO);
                                }
                            }
                        }
                    }
                    // Цикл работающий после аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals(Command.END)) {
                                out.writeUTF(Command.END);
                                break;
                            }
                            // Реализуем личные сообщения от пользователя к пользователю
                            if (str.startsWith(Command.PRIVATE_MSG)) {
                                String[] token = str.split("\\s+", 3); // Разделем на массив пользовательское сообщение. "\\s+" - для определения нескольких пробелов. Limit 3 - команда /w, имя получателя и остальное сообщение
                                // Случай если сообщение меньше требуемой длины, например указапнп команда и получатель, но нет самого сообщения. Такие сообщения просто пропускаем
                                if (token.length < 3) {
                                    continue;
                                }
                                server.privateMsg(this, token[1], token[2]); // Определяем от кого сообщение
                            }
                        } else {
                            server.broadcastMsg(this, str); // Если нет служебных комманд, то отправляем сообщение все пользователям
                        }
                    }
                } catch (SocketTimeoutException | RuntimeException e) {
                    System.out.println("Время для подключения к серверу истекло");
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

    public String getLogin() {
        return login;
    }
}
