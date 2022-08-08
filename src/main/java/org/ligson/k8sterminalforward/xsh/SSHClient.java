package org.ligson.k8sterminalforward.xsh;

import org.ligson.k8sterminalforward.web.SshTerminalEndpoint;
import org.springframework.stereotype.Component;

@Component
public class SSHClient {

    private final SshTerminalEndpoint sshTerminalEndpoint;

    public SSHClient(SshTerminalEndpoint sshTerminalEndpoint) {
        this.sshTerminalEndpoint = sshTerminalEndpoint;
    }

    public SSHConnection getConnection(String host, int port, String user, String password, String sid) {
        return new SSHConnection(host, port, user, password, sid, sshTerminalEndpoint);
    }
}
