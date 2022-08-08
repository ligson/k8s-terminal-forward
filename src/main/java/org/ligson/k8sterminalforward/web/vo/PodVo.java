package org.ligson.k8sterminalforward.web.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PodVo {
    private String name;
    private List<ContainerVo> containerVo = new ArrayList<>();
}
