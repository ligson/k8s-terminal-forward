package org.ligson.k8sterminalforward.web.vo;

import lombok.Data;

@Data
public class SshReqVo {
    private String host;
    private int port;
    private String user;
    private String password;
}
