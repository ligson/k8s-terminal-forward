package org.ligson.k8sterminalforward.web.vo;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ApiResult {
    private boolean success;
    private Map<String, Object> data = new HashMap<>();
    private String errorMsg;
    private String exceptionStack;

    public void put(String key, Object obj) {
        data.put(key, obj);
    }

}
