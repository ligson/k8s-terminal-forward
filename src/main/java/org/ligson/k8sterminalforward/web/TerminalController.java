package org.ligson.k8sterminalforward.web;

import org.ligson.k8sterminalforward.component.WebSocketSessionManager;
import org.ligson.k8sterminalforward.k8sclient.K8SClient;
import org.ligson.k8sterminalforward.web.vo.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/terminal")
public class TerminalController {
    private final WebSocketSessionManager webSocketSessionManager;
    private final K8SClient k8SClient;

    public TerminalController(WebSocketSessionManager webSocketSessionManager, K8SClient k8SClient) {
        this.webSocketSessionManager = webSocketSessionManager;
        this.k8SClient = k8SClient;
    }

    @PostMapping("/execInit")
    public ApiResult execInit(@RequestBody ExecParamVo execParamVo) {

        String token = "eyJhbGciOiJSUzI1NiIsImtpZCI6InZsZ2V0NnMtNGdCZ0ZJNWN3b3UwY3Zpb3ZwMGRfZkppZ1B5V21RdXNVSUkifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrOHMtY3lrLWFkbWluLXRva2VuLTdmbGpiIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6Ims4cy1jeWstYWRtaW4iLCJrdWJlcm5ldGVzLmlvL3NlcnZpY2VhY2NvdW50L3NlcnZpY2UtYWNjb3VudC51aWQiOiJkOWRhMmQ3Yi04OWM1LTQ2ZjUtYjhmZS0zMTM4YmMwYjgxYTAiLCJzdWIiOiJzeXN0ZW06c2VydmljZWFjY291bnQ6a3ViZS1zeXN0ZW06azhzLWN5ay1hZG1pbiJ9.sD6DnGc69Y55Bc_bNAwmOMm72l1f77JmrWLbi-81gm48MRhA1P18YeCAHTZL10hrPZgfPU23-sJz6XPktBbFP-TUKNrMIgO64vGptlYqg8S0UBYi_yY9iuod-hChRmbyvFxOajDf1nv23fXHN2AcJ0xMS4GOQICmTKKoNRYBiJHiaucvpJyc8R3qtgUkGrXONvAcQ_Zsm8v0EfDdODKwhl1ot8SeUCeIGWnW6FsQ4PwK3dqSDH2ljxVuKov7H4aINz-rQc05FspihLbqAkznrtLla_y0qYtOLS-YUpKENjKTbDBV3DbQA7yLqZI2GBfGUBHR6FfB0QU3uYEC5unLlA";
        String host = "wss://10.16.24.50:6443";
        String namespace = "pdev";
        String pod = "crux-auth-97c8d9458-5bcp5";
        String container = "crux-auth";
        String command = "sh";

        String id = UUID.randomUUID().toString();
        webSocketSessionManager.putExecParamVoMap(id, execParamVo);
        ApiResult apiResult = new ApiResult();
        apiResult.setSuccess(true);
        apiResult.put("sid", id);
        return apiResult;
    }

    @PostMapping("/namespaces")
    public ApiResult listNamespace(@RequestBody ListNameSpaceVo listNameSpaceVo) {
        List<String> nss = k8SClient.namespaces(listNameSpaceVo);
        ApiResult apiResult = new ApiResult();
        apiResult.setSuccess(true);
        apiResult.put("namespaces", nss);
        return apiResult;
    }

    @PostMapping("/pods")
    public ApiResult pods(@RequestBody ListPodVo ListPodVo) {
        List<PodVo> podVos = k8SClient.pods(ListPodVo);
        ApiResult apiResult = new ApiResult();
        apiResult.setSuccess(true);
        apiResult.put("pods", podVos);
        return apiResult;
    }

    @PostMapping("/accountToken")
    public ApiResult pods(@RequestBody GetAccountToken getAccountToken) {
        String token = k8SClient.accountToken(getAccountToken);
        ApiResult apiResult = new ApiResult();
        apiResult.setSuccess(true);
        apiResult.put("token", token);
        return apiResult;
    }
}
