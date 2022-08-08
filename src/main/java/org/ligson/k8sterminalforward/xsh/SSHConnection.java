package org.ligson.k8sterminalforward.xsh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ligson.k8sterminalforward.web.SshTerminalEndpoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Data
public class SSHConnection {
    // 创建JSch对象
    private final JSch jSch;
    private String host;
    private int port;
    private String user;
    private String password;
    private Session jSchSession;
    private Channel channel;
    private ExecutorService pool = Executors.newCachedThreadPool();
    private String sid;
    private boolean running = false;
    private SshTerminalEndpoint sshTerminalEndpoint;

    public SSHConnection(String host,
                         int port,
                         String user,
                         String password,
                         String sid,
                         SshTerminalEndpoint sshTerminalEndpoint) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        jSch = new JSch();
        this.sid = sid;
        this.sshTerminalEndpoint = sshTerminalEndpoint;
    }

    public void close() {
        // 关闭jschSesson流
        if (jSchSession != null && jSchSession.isConnected()) {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            jSchSession.disconnect();
        }
    }

    private void readMessage() throws IOException {
        InputStream input = channel.getInputStream();
        running = true;
        pool.submit(() -> {


            while (true) {
                if (!running) {
                    break;
                }
                try {
                    int n = 0;
                    byte[] buffer = new byte[1024];
                    if ((n = input.read(buffer)) != -1) {
                        byte[] result = new byte[n];
                        System.arraycopy(buffer, 0, result, 0, n);
                        sshTerminalEndpoint.replyMsg(sid, result);
                    }
                } catch (IOException e) {
                    log.error("read error :{}", e.getMessage(), e);
                    break;
                }

            }
        });
    }

    public void sendCmd(byte[] cmd) throws IOException {
        OutputStream oos = channel.getOutputStream();
        oos.write(cmd);
        oos.flush();
    }

    public boolean connect() {
        boolean result = false;
        try {
            // 根据主机账号、ip、端口获取一个Session对象
            jSchSession = jSch.getSession(user, host, port);
            // 存放主机密码
            jSchSession.setPassword(password);
            Properties config = new Properties();
            // 去掉首次连接确认
            config.put("StrictHostKeyChecking", "no");
            jSchSession.setConfig(config);
            // 超时连接时间为3秒
            jSchSession.setTimeout(3000);
            // 进行连接
            jSchSession.connect();
            // 获取连接结果
            result = jSchSession.isConnected();
            channel = jSchSession.openChannel("shell");
            channel.connect(3000);
            readMessage();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        if (result) {
            log.info("【SSH连接】连接成功");
        } else {
            log.error("【SSH连接】连接失败");
        }

        return result;
    }
}
