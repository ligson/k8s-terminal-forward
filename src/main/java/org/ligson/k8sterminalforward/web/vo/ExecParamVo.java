package org.ligson.k8sterminalforward.web.vo;

import lombok.Data;

@Data
public class ExecParamVo {
    private String scheme;
    private String host;
    private String port;
    private String namespace;
    private String podName;
    private String containerName;
    private String token;
}
