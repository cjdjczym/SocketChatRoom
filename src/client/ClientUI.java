package client;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

public class ClientUI implements ActionListener {
    public static String AUDIO_PATH = "C:\\Users\\cjdjczym\\Desktop";
    private static final String PCM = "audio.pcm";
    private static final String WAV = "audio.wav";

    public Client client;
//        public ClientV2 client;
    private String username;
    private static final int MAX_MSG = 1024;
    private String[] msgList;
    private int index;

    private JFrame jFrame;
    private JPanel jContentPane;
    private JList<String> jMsgList;
    private JScrollPane msgListPane;
    private JTextField msgTF;
    private JButton sendBtn;
    private JButton audioBtn;
    private JButton downloadAudioBtn;
    private JButton leaveBtn;

    public void createClient(String username, String ip, int port) {
        try {
            client = new Client(username, ip, port, this);
//            client = new ClientV2(username, ip, port, this);
            this.username = username;
            msgList = new String[MAX_MSG];
            index = 0;
            getJFrame().setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutDownClient() {
        try {
            client.disconnect();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void showMsg(String msg) {
        try{
            this.msgList[index++] = msg;
            jMsgList.setListData(msgList);
        }
        catch (Exception ignore){}
    }

    private JFrame getJFrame() {
        if (jFrame == null) {
            jFrame = new JFrame();
            jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            jFrame.setResizable(false);
            jFrame.setLocation(new Point(500, 265));
            jFrame.setJMenuBar(new JMenuBar());
            jFrame.setSize(350, 400);
            jFrame.setContentPane(getJContentPane());
            jFrame.setTitle("聊天室客户端");
        }
        return jFrame;
    }

    private JPanel getJContentPane() {
        if (jContentPane == null) {
            JLabel chatRoomLabel = new JLabel();
            chatRoomLabel.setBounds(new Rectangle(20, 15, 200, 25));
            chatRoomLabel.setText("聊天室名称：" + client.chatRoomName);

            JLabel userLabel = new JLabel();
            userLabel.setBounds(new Rectangle(20, 290, 100, 25));
            userLabel.setText(username + ": ");

            jContentPane = new JPanel();
            jContentPane.setLayout(null);
            jContentPane.setBackground(Color.LIGHT_GRAY);

            jContentPane.add(chatRoomLabel, null);
            jContentPane.add(userLabel, null);
            jContentPane.add(getMsgListPane(), null);
            jContentPane.add(getMsgTF(), null);
            jContentPane.add(getSendBtn(), null);
            jContentPane.add(getAudioBtn(), null);
            jContentPane.add(getDownloadAudioBtn(), null);
            jContentPane.add(getLeaveBtn(), null);
        }
        return jContentPane;
    }

    private JScrollPane getMsgListPane() {
        if (msgListPane == null) {
            jMsgList = new JList<>(msgList);
            msgListPane = new JScrollPane(jMsgList);
            msgListPane.setBounds(new Rectangle(20, 90, 290, 195));
        }
        return msgListPane;
    }

    private JTextField getMsgTF() {
        if (msgTF == null) {
            msgTF = new JTextField();
            msgTF.setBounds(new Rectangle(20, 315, 222, 30));
        }
        return msgTF;
    }

    private JButton getSendBtn() {
        if (sendBtn == null) {
            sendBtn = new JButton();
            sendBtn.setBounds(new Rectangle(255, 315, 65, 30));
            sendBtn.addActionListener(this);
            sendBtn.setText("发送");
        }
        return sendBtn;
    }

    private JButton getAudioBtn() {
        if (audioBtn == null) {
            audioBtn = new JButton();
            audioBtn.setBounds(new Rectangle(20, 50, 85, 30));
            audioBtn.addActionListener(this);
            audioBtn.setText("开始录制");
        }
        return audioBtn;
    }

    private JButton getDownloadAudioBtn() {
        if (downloadAudioBtn == null) {
            downloadAudioBtn = new JButton();
            downloadAudioBtn.setBounds(new Rectangle(115, 50, 85, 30));
            downloadAudioBtn.addActionListener(this);
            downloadAudioBtn.setText("播放语音");
        }
        return downloadAudioBtn;
    }

    private JButton getLeaveBtn() {
        if (leaveBtn == null) {
            leaveBtn = new JButton();
            leaveBtn.setBounds(new Rectangle(255, 10, 65, 30));
            leaveBtn.addActionListener(this);
            leaveBtn.setText("离开");
        }
        return leaveBtn;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            if ("发送".equals(e.getActionCommand())) {
                if ("".equals(msgTF.getText().trim()))
                    showDialog("聊天内容不能为空");
                else if (msgTF.getText().startsWith("upload")) {
                    client.sendFile(msgTF.getText().split(" ")[1]);
                    msgTF.setText("");
                } else if (msgTF.getText().startsWith("download")) {
                    client.receiveFile(msgTF.getText().substring(9));
                    msgTF.setText("");
                } else {
                    client.speak(msgTF.getText());
                    msgTF.setText("");
                }
            } else if ("开始录制".equals(e.getActionCommand())) {
                audioBtn.setText("停止");
                new Thread(() -> {
                    try {
                        AudioRecord.save(AUDIO_PATH + PCM);
                    } catch (IOException | LineUnavailableException ioException) {
                        ioException.printStackTrace();
                    }
                }).start();
            } else if ("停止".equals(e.getActionCommand())) {
                AudioRecord.running = false;
                String[] args = new String[2];
                args[0] = AUDIO_PATH + PCM;
                args[1] = AUDIO_PATH + WAV;
                Pcm2Wav.convertAudioFiles(args);
                client.sendFile(AUDIO_PATH + WAV);
                File pcmFile = new File(AUDIO_PATH + PCM);
                File wavFile = new File(AUDIO_PATH + WAV);
                audioBtn.setText("开始录制");
                pcmFile.delete();
                wavFile.delete();
            } else if ("播放语音".equals(e.getActionCommand())) {
                for (int i = index - 1; i >= 0; i--) {
                    if (msgList[i].contains("audio.wav")) {
                        String audioName = msgList[i].substring(16, msgList[i].lastIndexOf("] [Size: "));
                        client.receiveFile(audioName + " " + AUDIO_PATH);
                        Thread.sleep(500);
                        AudioPlayer.playAndDelete(AUDIO_PATH + File.separatorChar + audioName);
                        break;
                    }
                }
            } else {
                getJFrame().setVisible(false);
                shutDownClient();
            }
        } catch (IOException | InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    public void showDialog(String msg) {
        JOptionPane.showMessageDialog(jFrame, msg);
    }
}
