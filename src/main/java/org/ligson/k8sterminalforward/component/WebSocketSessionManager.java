package org.ligson.k8sterminalforward.component;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ligson.k8sterminalforward.k8sclient.Connection;
import org.ligson.k8sterminalforward.k8sclient.K8SClient;
import org.ligson.k8sterminalforward.web.vo.ExecParamVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.Session;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketSessionManager {
    private Map<String, ExecParamVo> execParamVoMap = new ConcurrentHashMap<>();

    private Map<String, Session> sidSessionMap = new ConcurrentHashMap<>();

    private Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    private Map<String, Connection> connectionMap = new ConcurrentHashMap<>();
    @Autowired
    private K8SClient k8SClient;

    public void putExecParamVoMap(String sid, ExecParamVo execParamVo) {
        execParamVoMap.put(sid, execParamVo);
    }

    public ExecParamVo getExecParamVoMap(String sid) {
        return execParamVoMap.get(sid);
    }

    public void close(String sid) {
        Session session = sidSessionMap.get(sid);
        if (session != null) {
            sessionMap.remove(session.getId());
        }

        execParamVoMap.remove(sid);
        sidSessionMap.remove(sid);

        Connection conn = connectionMap.get(sid);
        if (conn != null) {
            conn.close();
        }
    }

    public void open(String sid, Session session) throws Exception {
        sidSessionMap.put(sid, session);
        sessionMap.put(session.getId(), session);
        ExecParamVo param = getExecParamVoMap(sid);
        Connection conn = k8SClient.getConnection(param.getScheme(), param.getHost(), param.getPort(), param.getNamespace(), param.getPodName(), param.getContainerName(), param.getToken(), this, sid);
        connectionMap.put(sid, conn);
        conn.connect();
    }

    public void reply(String msg, String sid) {
        Session session = sidSessionMap.get(sid);
        if (session.isOpen()) {
            try {
                session.getBasicRemote().sendText(msg);
            } catch (IOException e) {
                log.error("把信息:{}发送给会话:{}失败,原因:{},stack:{}", msg, session.getId(), e.getMessage(), ExceptionUtils.getStackTrace(e));
            }
        } else {
            log.error("会话:{}已关闭", session.getId());
        }
    }

    public void onMessage(String sid, String message) {
        Connection conn = connectionMap.get(sid);
        if (conn != null) {
            conn.receiveMsg(message);
        }

    }
}
