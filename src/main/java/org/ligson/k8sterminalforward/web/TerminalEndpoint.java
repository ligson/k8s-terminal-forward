package org.ligson.k8sterminalforward.web;

import lombok.extern.slf4j.Slf4j;
import org.ligson.k8sterminalforward.component.WebSocketSessionManager;
import org.ligson.k8sterminalforward.util.ServiceLocator;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

@Component
@ServerEndpoint("/terminal/{sid}")
@Slf4j
public class TerminalEndpoint {
    private WebSocketSessionManager webSocketSessionManager;

    public WebSocketSessionManager getWebSocketSessionManager() {
        if (webSocketSessionManager == null) {
            webSocketSessionManager = ServiceLocator.getService(WebSocketSessionManager.class);
        }
        return webSocketSessionManager;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) throws Exception {
        getWebSocketSessionManager().open(sid, session);
    }

    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        getWebSocketSessionManager().close(sid);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        log.info("Receive a message from client: " + message);
        getWebSocketSessionManager().onMessage(sid, message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("Error while websocket. ", error);
    }


}
