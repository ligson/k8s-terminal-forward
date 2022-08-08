package org.ligson.k8sterminalforward.xsh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class SSHConnection {
    public static boolean connect(String username, String password, String ip, int port) {
        // 创建JSch对象
        JSch jSch = new JSch();
        Session jSchSession = null;

        boolean reulst = false;

        try {
            // 根据主机账号、ip、端口获取一个Session对象
            jSchSession = jSch.getSession(username, ip, port);
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
            reulst = jSchSession.isConnected();
        } catch (JSchException e) {
            log.error(e.getMessage());
        } finally {
            // 关闭jschSesson流
            if (jSchSession != null && jSchSession.isConnected()) {
                jSchSession.disconnect();
            }
        }
        if (reulst) {
            log.error("【SSH连接】连接成功");
        } else {
            log.error("【SSH连接】连接失败");
        }
        return reulst;
    }

    public static void main(String[] args) {
        String username = "root";
        //String password = "123456";
        String host = "10.16.24.85";
        int port = 49622;
        ExecutorService pool = Executors.newCachedThreadPool();
        String pat = "`1234567890-=qwertyuiopasdfghjklzxcvbnm,./;'[]~!@#$%^&*()_+";
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            pool.submit(() -> {
                for (int j = 0; j < 100; j++) {
                    String password = RandomStringUtils.random(14,pat.toCharArray());
                    log.debug("线程{}第{}次尝试,使用密码:{}", finalI, j, password);
                    boolean r = connect(username, password, host, port);
                    if (r) {
                        break;
                    }
                }
            });
        }

    }
}
