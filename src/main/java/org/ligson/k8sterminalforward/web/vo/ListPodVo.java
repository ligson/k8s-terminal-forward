package org.ligson.k8sterminalforward.web.vo;

import lombok.Data;

@Data
public class ListPodVo {
    private String apiServerUrl;
    private String token;
    private String namespace;
}
