import Server.ChatRoom;
import client.ClientUI;

public class Test {
    public static void main(String[] args) {
        ChatRoom.create("随便聊聊吧", 9090);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ClientUI c1 = new ClientUI();
        c1.createClient("cjcj", "127.0.0.1", 9090);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ClientUI c2 = new ClientUI();
        c2.createClient("djdj", "127.0.0.1", 9090);

//        Server.ChatRoom.close();
    }
}
