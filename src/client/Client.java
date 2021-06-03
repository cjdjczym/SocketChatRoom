package client;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class Client {
    private final String ip;
    private final int port;
    private final ClientUI UI;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private DataOutputStream dos;
    private String savePath;
    public String chatRoomName;

    public Client(String username, String ip, int port, ClientUI UI) throws IOException {
        this.ip = ip;
        this.port = port;
        this.UI = UI;
        connect(username);
    }

    public void connect(String username) throws IOException {
        socket = new Socket(ip, port);

        // 关闭缓冲区，及时发送数据
        if (!socket.getTcpNoDelay()) socket.setTcpNoDelay(true);
        // 若长时间没有连接则断开socket
        if (!socket.getKeepAlive()) socket.setKeepAlive(true);
        // 允许发送紧急数据，不做处理
        if (!socket.getOOBInline()) socket.setOOBInline(true);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream());
        dos = new DataOutputStream(socket.getOutputStream());
        speak(username);
        chatRoomName = in.readLine();
        new Thread(() -> {
            boolean isFile = false;
            try {
                DataInputStream dis = new DataInputStream(socket.getInputStream());
                while (true) {
                    if (isFile) {
                        String fileName = dis.readUTF();
                        long fileLen = dis.readLong();
                        File directory = new File(savePath);
                        File file = new File(directory.getAbsolutePath() + File.separatorChar + fileName);
                        FileOutputStream fos = new FileOutputStream(file);
                        byte[] bytes = new byte[1024];
                        int total = 0;
                        int length;
                        while (total < fileLen && (length = dis.read(bytes, 0, bytes.length)) != -1) {
                            total += length;
                            fos.write(bytes, 0, length);
                            fos.flush();
                        }
                        fos.close();
                        isFile = false;
                        UI.showDialog("文件" + fileName + "接收成功");
                        continue;
                    }
                    String str = in.readLine();
                    if (str == null) break;
                    if ("download".equals(str)) isFile = true;
                    else UI.showMsg(str);
                }
            } catch (IOException e) {
                if (!(e instanceof SocketException)) e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            final String heartbeat = "[usage for heartbeat packet]";
            while (true) {
                try {
                    Thread.sleep(15 * 1000); // 每15s发送一次心跳
                    out.write(heartbeat + "\r\n");
                    out.flush();
                    try {
                        socket.sendUrgentData(0xFF);
                    } catch (IOException ex) {
                        UI.showDialog("网络连接已断开");
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void disconnect() throws IOException, InterruptedException {
        speak("exit");
        Thread.sleep(1000); // 模拟断连的处理耗时
        socket.shutdownInput();
        socket.shutdownOutput();
        socket.close();
    }

    public void speak(String str) {
        out.write(str + "\r\n");
        out.flush();
    }

    public void sendFile(String path) throws IOException, InterruptedException {
        File file = new File(path);
        if (!file.exists()) return;
        speak("upload");
        Thread.sleep(500);
        FileInputStream fis = new FileInputStream(file);
        dos.writeUTF(file.getName());
        dos.flush();
        dos.writeLong(file.length());
        dos.flush();
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes, 0, bytes.length)) != -1) {
            dos.write(bytes, 0, length);
            dos.flush();
        }
        fis.close();
    }

    public void receiveFile(String str) {
        this.savePath = str.split(" ")[1];
        speak("download " + str.split(" ")[0]);
    }
}