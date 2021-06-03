package Server;

import java.io.*;
import java.math.RoundingMode;
import java.net.Socket;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.util.Date;

public class MsgTask implements Runnable {
    private static final String HEARTBEAT = "[usage for heartbeat packet]";
    private final Server server;
    private final String localPath;
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final DataOutputStream dos;
    private final String username;
    private int fileCount;
    private boolean connected;
    private long timestamp; // 上次接受数据的时间，用于心跳包检测
    private int timeout; // 超时次数，到达3次则断开该socket连接

    public MsgTask(Server server, String localPath, Socket socket, BufferedReader in, PrintWriter out, String username) throws IOException {
        this.server = server;
        this.localPath = localPath;
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.username = username;
        fileCount = 0;
        connected = true;
        timestamp = new Date().getTime();
        timeout = 0;
        dos = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        new Thread(() -> {
            while (connected) {
                try {
                    Thread.sleep(20 * 1000);
                    if (new Date().getTime() - timestamp > 20 * 1000) {
                        if (timeout == 2) disconnect();
                        else timeout++;
                    }
                } catch (InterruptedException | IOException e) {
                    if (!(e instanceof SocketException)) e.printStackTrace();
                }
            }
        }).start();
        boolean isFile = false;
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            while (connected) {
                if (isFile) {
                    fileCount++;
                    String fileName = dis.readUTF();
                    fileName = username + "_" + fileCount + "_" + fileName;
                    long fileLen = dis.readLong();
                    File directory = new File(localPath);
                    File file = new File(directory.getAbsolutePath() + File.separatorChar + fileName);
                    if (file.exists()) file.delete();
                    FileOutputStream fos = new FileOutputStream(file);
                    byte[] bytes = new byte[1024];
                    int length;
                    int total = 0;
                    while (total < fileLen && (length = dis.read(bytes, 0, bytes.length)) != -1) {
                        total += length;
                        fos.write(bytes, 0, length);
                        fos.flush();
                    }
                    timestamp = new Date().getTime();
                    server.broadcast(Server.ADMIN, "来自用户" + username + "的文件接收成功");
                    server.broadcast(Server.ADMIN, "[File Name: " + fileName + "] [Size: " + getFormatFileSize(fileLen) + "]");
                    fos.close();
                    isFile = false;
                    continue;
                }
                String str = in.readLine();
                timestamp = new Date().getTime();
                if ("exit".equals(str)) {
                    disconnect();
                    break;
                } else if ("upload".equals(str)) {
                    isFile = true;
                } else if (str != null && str.startsWith("download")) {
                    out.write("download\r\n");
                    out.flush();
                    Thread.sleep(500);
                    File file = new File(localPath + File.separatorChar + str.substring(9));
                    dos.writeUTF(file.getName());
                    dos.flush();
                    dos.writeLong(file.length());
                    dos.flush();
                    byte[] bytes = new byte[1024];
                    int length;
                    FileInputStream fis = new FileInputStream(file);
                    while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
                        dos.write(bytes, 0, length);
                        dos.flush();
                    }
                    fis.close();
                } else if (HEARTBEAT.equals(str)) {
//                    System.out.println();
//                    System.out.println(username + " heartbeat");
                    this.timeout = 0;
                } else if (str != null) {
                    server.broadcast(username, str);
                } else break;
            }
        } catch (IOException | InterruptedException e) {
            if (!(e instanceof SocketException
            )) e.printStackTrace();
        }
    }

    private void disconnect() throws IOException {
        synchronized (Server.sockets) {
            Server.sockets.remove(server.getAddress(socket));
        }
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
        server.broadcast(Server.ADMIN, "用户" + username + "离开了聊天室");
        ChatRoom.printStatus();
        connected = false;
    }

    /**
     * 格式化文件大小
     *
     * @param length data length
     * @return 文件大小
     */
    private String getFormatFileSize(long length) {
        DecimalFormat df = new DecimalFormat("#0.0");
        df.setRoundingMode(RoundingMode.HALF_UP);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(1);
        double size = ((double) length) / (1 << 30);
        if (size >= 1) {
            return df.format(size) + "GB";
        }
        size = ((double) length) / (1 << 20);
        if (size >= 1) {
            return df.format(size) + "MB";
        }
        size = ((double) length) / (1 << 10);
        if (size >= 1) {
            return df.format(size) + "KB";
        }
        return length + "B";
    }
}
