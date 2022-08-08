package org.ligson.k8sterminalforward.web;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.ligson.k8sterminalforward.util.ServiceLocator;
import org.ligson.k8sterminalforward.web.vo.SshReqVo;
import org.ligson.k8sterminalforward.xsh.SSHClient;
import org.ligson.k8sterminalforward.xsh.SSHConnection;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ServerEndpoint("/sshterminal/{sid}")
@Slf4j
public class SshTerminalEndpoint {
    private SSHClient sshClient;

    private final static Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    private final static Map<String, String> sidMap = new ConcurrentHashMap<>();
    private final static Map<String, SSHConnection> sshConnectionMap = new ConcurrentHashMap<>();
    private final static Map<String, SshReqVo> sshReqVoMap = new ConcurrentHashMap<>();

    public SSHClient getSshClient() {
        if (sshClient == null) {
            sshClient = ServiceLocator.getService(SSHClient.class);
        }
        return sshClient;
    }

    public void putSSHParam(String sid, SshReqVo sshReqVo) {
        sshReqVoMap.put(sid, sshReqVo);
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) throws Exception {
        sessionMap.put(session.getId(), session);
        sidMap.put(sid, session.getId());
        SshReqVo map = sshReqVoMap.get(sid);
        if (map != null) {
            SSHConnection conn = getSshClient().getConnection(map.getHost(), map.getPort(), map.getUser(), map.getPassword(), sid);
            conn.connect();
            sshConnectionMap.put(sid, conn);
        }
    }

    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        SSHConnection con = sshConnectionMap.get(sid);
        if (con != null) {
            con.close();
        }
        String sessionId = sidMap.get(sid);
        if (sessionId != null) {
            sessionMap.remove(sessionId);
        }
        sidMap.remove(sid);
        SSHConnection conn = sshConnectionMap.get(sid);
        if (conn != null) {
            conn.close();
        }
        sshConnectionMap.remove(sid);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("Receive a message from client: " + message);
        SSHConnection conn = sshConnectionMap.get(sid);
        if (conn != null) {
            if (message != null) {
                byte[] cmd = Base64.decodeBase64(message.substring(1));
                try {
                    conn.sendCmd(cmd);
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            }
        }
    }

    public void replyMsg(String sid, byte[] buffer) {
        String sessionId = sidMap.get(sid);
        if (sessionId != null) {
            Session session = sessionMap.get(sessionId);
            if (session != null && session.isOpen()) {
                try {
                    String result = "1" + Base64.encodeBase64String(buffer);
                    session.getBasicRemote().sendText(result);
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("Error while websocket. ", error);
    }


}
