package Server;

import java.io.IOException;
import java.util.*;

public class ChatRoom {
    public static String name;
    private static int localPort;
    public static List<String> msgList;
    private static Server server;
//    private static ServerV2 server;

    // 创建聊天室
    public static void create(String name, int localPort) {
        ChatRoom.name = name;
        ChatRoom.localPort = localPort;
        ChatRoom.msgList = new LinkedList<>();
        try {
            server = new Server(localPort, name);
//            server = new ServerV2(localPort, name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 关闭聊天室
    public static void close() {
        if(ChatRoom.name == null) {
            System.out.println("错误：聊天室未开放");
            return;
        }
        try {
            server.shutdown();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void printStatus() {
        if (server == null) {
            System.out.println("Error：聊天室未被创建");
            return;
        }
        System.out.println();
        System.out.println(name + "聊天室：");
        System.out.println("当前人数" + server.currentClients() + "人 | 端口号" + localPort);
        System.out.println("----------------------------");
        for (String msg : msgList) System.out.println(msg);
    }

    public static void saveMsg(String msg) {
        msgList.add(msg);
    }
}
