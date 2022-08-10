package org.ligson.k8sterminalforward.web.vo;

import lombok.Data;

@Data
public class GetAccountToken {
    private String apiServerUrl;
    private String token;
    private String namespace = "kube-system";
}
