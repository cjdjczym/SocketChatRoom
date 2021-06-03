package client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

public class ClientV2 {
    private final String username;
    private final String ip;
    private final int port;
    private final ClientUI UI;
    private SocketChannel socketChannel;
    public String chatRoomName;
    private static final Charset charset = StandardCharsets.UTF_8;

    public ClientV2(String username, String ip, int port, ClientUI UI) throws IOException {
        this.username = username;
        this.ip = ip;
        this.port = port;
        this.UI = UI;
        connect();
    }

    private void connect() throws IOException {
        Selector selector = Selector.open();
        socketChannel = SocketChannel.open(new InetSocketAddress(ip, port));
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        speak(null);
        new Thread(() -> {
            try {
                while (selector.select() > 0) {
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();  // 可以通过这个方法，知道可用通道的集合
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey sk = keyIterator.next();
                        keyIterator.remove();
                        if (sk.isReadable()) {
                            String str = read((SocketChannel) sk.channel(), sk);
                            if (str.equals("")) continue;
                            if (chatRoomName == null) chatRoomName = str;
                            else UI.showMsg(str);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void disconnect() throws IOException, InterruptedException {
        speak("exit");
        Thread.sleep(1000);
        socketChannel.close();
    }

    public void speak(String str) throws IOException {
        if (str == null) socketChannel.write(charset.encode(username));
        else socketChannel.write(charset.encode(username + " " + str));
    }

    private String read(SocketChannel sc, SelectionKey sk) throws IOException {
        ByteBuffer buff = ByteBuffer.allocate(1024);
        StringBuilder content = new StringBuilder();
        while (sc.read(buff) > 0) {
            buff.flip();
            content.append(charset.decode(buff));
        }
        sk.interestOps(SelectionKey.OP_READ);
        return content.toString();
    }

    public void sendFile(String path) throws IOException, InterruptedException {
        // TODO
    }

    public void receiveFile(String str) {
        // TODO
    }
}
