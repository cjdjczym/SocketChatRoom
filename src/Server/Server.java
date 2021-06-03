package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class Server {
    public static String SAVE_PATH = "D:\\IntelliJ IDEA\\SocketChatRoom\\src\\example";

    public static final Map<String, Socket> sockets = new LinkedHashMap<>();
    public static final String ADMIN = "系统";
    private final int localPort;
    private ServerSocket server;
    private final String chatRoomName;

    /**
     * 创建服务端并开始监听
     *
     * @param localPort 监听的本地端口
     * @throws IOException IO操作
     */
    public Server(int localPort, String chatRoomName) throws IOException {
        this.localPort = localPort;
        this.chatRoomName = chatRoomName;
        startListen();
    }

    private void startListen() throws IOException {
        server = new ServerSocket(localPort);
        new Thread(() -> {
            try {
                while (true) {
                    Socket socket = server.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String username = in.readLine();
                    PrintWriter out = new PrintWriter(socket.getOutputStream());
                    out.write(chatRoomName + "\r\n");
                    out.flush();
                    synchronized (sockets) {
                        broadcast(ADMIN, "欢迎用户" + username + "进入聊天室");
                        sockets.put(getAddress(socket), socket);
                        ChatRoom.printStatus();
                    }
                    new Thread(new MsgTask(this, SAVE_PATH, socket, in, out, username)).start();
                }
            } catch (IOException e) {
                if (!(e instanceof SocketException)) e.printStackTrace();
            }
        }).start();
    }

    /**
     * 获取当前服务端连接的Socket数量
     *
     * @return Socket数量
     */
    public int currentClients() {
        return sockets.size();
    }

    /**
     * 关闭服务端
     *
     * @throws IOException          IO操作
     * @throws InterruptedException 用到了[Thread.sleep]来模拟处理耗时
     */

    public void shutdown() throws IOException, InterruptedException {
        if (server == null) return;
        broadcast(ADMIN, "聊天室即将关闭");
        Thread.sleep(1000); // 模拟关闭的处理耗时
        synchronized (sockets) {
            for (Socket socket : sockets.values()) {
                socket.shutdownInput();
                socket.shutdownOutput();
                socket.close();
            }
            sockets.clear();
        }
        server.close();
    }

    /**
     * 向服务端中的所有Socket连接发送广播消息
     *
     * @param from 广播的发起人
     * @param msg  广播信息
     * @throws IOException IO操作
     */
    public void broadcast(String from, String msg) throws IOException {
        ChatRoom.saveMsg(from + ": " + msg); // 通知聊天室储存聊天消息
        PrintWriter out;
        synchronized (sockets) {
            for (Socket socket : sockets.values()) {
                out = new PrintWriter(socket.getOutputStream());
                out.write(from + ": " + msg + "\r\n");
                out.flush();
            }
        }
    }

    public String getAddress(Socket socket) {
        return socket.getInetAddress() + ":" + socket.getPort();
    }
}
