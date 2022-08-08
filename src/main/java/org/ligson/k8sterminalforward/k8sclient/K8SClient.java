package org.ligson.k8sterminalforward.k8sclient;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.ligson.k8sterminalforward.component.WebSocketSessionManager;
import org.ligson.k8sterminalforward.web.vo.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class K8SClient {

    public Connection getExecConnection(String scheme, String host, String port, String namespace, String podName, String containerName, String token, WebSocketSessionManager webSocketSessionManager, String sid) {
        String url = scheme + "://" + host + ":" + port + "/api/v1/namespaces/" + namespace + "/pods/" + podName + "/exec?container=" + containerName + "&stdin=true&stdout=true&stderr=true&tty=true&command=sh&pretty=true&follow=true";
        log.debug("websocket url:{},token:{}", url, token);
        return new Connection(url, token, webSocketSessionManager, sid);
    }

    private Config getK8SConfig(String k8sApiUrl, String userToken) {
        String adminConfData = "apiVersion: v1\n" +
                "clusters:\n" +
                "- cluster:\n" +
                "    insecure-skip-tls-verify: true\n" +
                "    server: " + k8sApiUrl + "\n" +
                "  name: govdev\n" +
                "contexts:\n" +
                "- context:\n" +
                "    cluster: govdev\n" +
                "    user: govdev\n" +
                "  name: govdev\n" +
                "current-context: govdev\n" +
                "kind: Config\n" +
                "users:\n" +
                "- name: govdev\n" +
                "  user:\n" +
                "    token: " + userToken;
        Config config = Config.fromKubeconfig(adminConfData);
        return config;
    }

    public List<String> namespaces(ListNameSpaceVo listNameSpaceVo) {
        Config config = getK8SConfig(listNameSpaceVo.getApiServerUrl(), listNameSpaceVo.getToken());
        KubernetesClient k8s = new KubernetesClientBuilder().withConfig(config).build();

        NamespaceList nsList = k8s.namespaces().list();
        k8s.close();
        return nsList.getItems().stream().map(e -> e.getMetadata().getName()).collect(Collectors.toList());
    }

    public List<PodVo> pods(ListPodVo listPodVo) {
        List<PodVo> podVos = new ArrayList<>();
        Config config = getK8SConfig(listPodVo.getApiServerUrl(), listPodVo.getToken());
        KubernetesClient k8s = new KubernetesClientBuilder().withConfig(config).build();
        k8s.getConfiguration().setNamespace(listPodVo.getNamespace());
        for (Pod item : k8s.pods().list().getItems()) {
            PodVo podVo = new PodVo();
            podVo.setName(item.getMetadata().getName());
            for (Container container : item.getSpec().getContainers()) {
                ContainerVo containerVo = new ContainerVo();
                containerVo.setId(container.getName());
                podVo.getContainerVo().add(containerVo);
            }
            podVos.add(podVo);
        }
        return podVos;
    }

    public String accountToken(GetAccountToken getAccountToken) {
        return null;
    }
}
