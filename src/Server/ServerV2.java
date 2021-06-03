package Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.nio.channels.Channel;
import java.nio.charset.Charset;

public class ServerV2 {
    private final Selector selector;
    private ServerSocketChannel server;

    private final int localPort;
    private final String chatRoomName;
    private boolean leave = false;

    private static final Charset charset = StandardCharsets.UTF_8;
    private static final String ADMIN = "系统";

    public ServerV2(int localPort, String chatRoomName) throws IOException {
        this.localPort = localPort;
        this.chatRoomName = chatRoomName;
        selector = Selector.open();
        init();
    }

    public void init() throws IOException {
        server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(localPort));
        // 非阻塞的方式
        server.configureBlocking(false);
        // 注册到选择器上，设置为监听状态
        server.register(selector, SelectionKey.OP_ACCEPT);
        new Thread(() -> {
            try {
                while (selector.select() > 0) {
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();  // 可以通过这个方法，知道可用通道的集合
                    Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                    while (keyIterator.hasNext()) {
                        SelectionKey sk = keyIterator.next();
                        keyIterator.remove();
                        dealWithSelectionKey(server, sk);
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void dealWithSelectionKey(ServerSocketChannel server, SelectionKey sk) throws IOException, InterruptedException {
        if (sk.isAcceptable()) {
            SocketChannel sc = server.accept();
            // 非阻塞模式
            sc.configureBlocking(false);
            // 注册选择器，并设置为读取模式，收到一个连接请求，然后起一个SocketChannel，并注册到selector上，之后这个连接的数据，就由这个SocketChannel处理
            sc.register(selector, SelectionKey.OP_READ);

            // 将此对应的channel设置为准备接受其他客户端请求
            sk.interestOps(SelectionKey.OP_ACCEPT);

            String username = read(sc);
            sc.write(charset.encode(chatRoomName));
            broadCast(selector, sc, ADMIN, "欢迎用户" + username + "进入聊天室");
            ChatRoom.printStatus();
        }
        // 处理来自客户端的数据读取请求
        if (sk.isReadable()) {
            String data = read((SocketChannel) sk.channel());
            sk.interestOps(SelectionKey.OP_READ);
            if (data.length() > 0) {
                if ("exit".equals(data.split(" ")[1])) {
                    broadCast(selector, (SocketChannel) sk.channel(), ADMIN, "用户" + data.split(" ")[0] + "离开了聊天室");
                    sk.channel().close();
                    leave = true;
                } else broadCast(selector, null, data.split(" ")[0], data.split(" ")[1]);
                ChatRoom.printStatus();
                leave = false;
            }
        }
    }

    public int currentClients() {
        int res = 0;
        for (SelectionKey key : selector.keys())
            if (key.channel() instanceof SocketChannel) res++;
        if (leave) res--;
        return res;
    }

    public void broadCast(Selector selector, SocketChannel except, String from, String content) throws IOException {
        ChatRoom.saveMsg(from + ": " + content);
        // 广播数据到所有的SocketChannel中
        for (SelectionKey key : selector.keys()) {
            Channel targetChannel = key.channel();
            // 如果except不为空，不回发给发送此内容的客户端
            if (targetChannel instanceof SocketChannel && targetChannel != except) {
                SocketChannel dest = (SocketChannel) targetChannel;
                dest.write(charset.encode(from + ": " + content));
            }
        }
    }

    private String read(SocketChannel sc) throws IOException {
        // 返回该SelectionKey对应的 Channel，其中有数据需要读取
        ByteBuffer buff = ByteBuffer.allocate(1024);
        StringBuilder content = new StringBuilder();
        while (sc.read(buff) > 0) {
            buff.flip();
            content.append(charset.decode(buff));
        }
        return content.toString();
    }

    public void shutdown() throws IOException, InterruptedException {
        if (server == null) return;
        broadCast(selector, null, ADMIN, "聊天室即将关闭");
        Thread.sleep(1000);
        server.close();
    }
}
