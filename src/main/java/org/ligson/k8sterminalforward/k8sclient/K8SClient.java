package org.ligson.k8sterminalforward.k8sclient;

import lombok.extern.slf4j.Slf4j;
import org.ligson.k8sterminalforward.component.WebSocketSessionManager;
import org.springframework.stereotype.Component;

import javax.websocket.Session;

@Slf4j
@Component
public class K8SClient {

    public Connection getConnection(String scheme, String host, String port, String namespace, String podName, String containerName, String token, WebSocketSessionManager webSocketSessionManager,String sid) {
        String url = scheme + "://" + host + ":" + port + "/api/v1/namespaces/" + namespace + "/pods/" + podName + "/exec?container=" + containerName + "&stdin=true&stdout=true&stderr=true&tty=true&command=sh&pretty=true&follow=true";
        log.debug("websocket url:{},token:{}", url, token);
        return new Connection(url, token, webSocketSessionManager,sid);
    }
}
