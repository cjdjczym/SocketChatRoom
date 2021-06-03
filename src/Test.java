import Server.ChatRoom;
import client.ClientUI;

import java.util.Scanner;

public class Test {
    public static void main(String[] args) {
        System.out.println("------------使用说明------------");
        System.out.println("创建聊天室：create 聊天室名称 监听端口");
        System.out.println("连入聊天室：user 用户名 聊天室IP 聊天室端口");
        System.out.println("关闭聊天室：close");
        System.out.println("-------------------------------");
        new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            String line = sc.nextLine();
            while (!"exit".equals(line)) {
                if (line.startsWith("create"))
                    ChatRoom.create(line.split(" ")[1], Integer.parseInt(line.split(" ")[2]));
                else if (line.startsWith("user")) {
                    ClientUI clientUI = new ClientUI();
                    clientUI.createClient(line.split(" ")[1], line.split(" ")[2], Integer.parseInt(line.split(" ")[3]));
                } else if (line.startsWith("close")) ChatRoom.close();
                line = sc.nextLine();
            }
        }).start();
    }
}
