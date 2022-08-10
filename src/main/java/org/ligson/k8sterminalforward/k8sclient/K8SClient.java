package org.ligson.k8sterminalforward.k8sclient;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.ligson.k8sterminalforward.component.WebSocketSessionManager;
import org.ligson.k8sterminalforward.web.vo.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class K8SClient {
    static final private String ACCOUNT_SERVICE_NAME = "crux-cisupport-container-terminal";

    public Connection getExecConnection(String scheme, String host, String port, String namespace, String podName, String containerName, String token, WebSocketSessionManager webSocketSessionManager, String sid) {
        String url = "";
        if(null == port){
            if(host.contains("/")){
                host = host.split("/")[0];
            }
            url = scheme + "://" + host + "/api/v1/namespaces/" + namespace + "/pods/" + podName + "/exec?container=" + containerName + "&stdin=true&stdout=true&stderr=true&tty=true&command=sh&pretty=true&follow=true";
        }else{
            url = scheme + "://" + host + ":" + port + "/api/v1/namespaces/" + namespace + "/pods/" + podName + "/exec?container=" + containerName + "&stdin=true&stdout=true&stderr=true&tty=true&command=sh&pretty=true&follow=true";
        }
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
        Config config = getK8SConfig(getAccountToken.getApiServerUrl(), getAccountToken.getToken());
        KubernetesClient client = new KubernetesClientBuilder().withConfig(config).build();
        ServiceAccountList serviceAccountList = client.serviceAccounts().inNamespace(getAccountToken.getNamespace()).list();
        List<ServiceAccount> serviceAccounts = serviceAccountList.getItems().stream().filter(serviceAccount -> serviceAccount.getMetadata().getName().equals(ACCOUNT_SERVICE_NAME)).collect(Collectors.toList());
        if(null == serviceAccounts || serviceAccounts.size() == 0){
            creatServiceAccount(client,getAccountToken.getNamespace());
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            serviceAccountList = client.serviceAccounts().inNamespace(getAccountToken.getNamespace()).list();
            serviceAccounts = serviceAccountList.getItems().stream().filter(serviceAccount -> serviceAccount.getMetadata().getName().equals(ACCOUNT_SERVICE_NAME)).collect(Collectors.toList());
        }
        String accountTokenName = serviceAccounts.get(0).getSecrets().get(0).getName();
        Secret secret = client.secrets().inNamespace(getAccountToken.getNamespace()).withName(accountTokenName).get();
        String token = secret.getData().get("token");
        byte[] decode = Base64.getDecoder().decode(token);
        return new String(decode);
    }

    private void creatServiceAccount(KubernetesClient client, String namespace) {
        ServiceAccount sa = new ServiceAccountBuilder().withNewMetadata().withName("crux-cisupport-container-terminal")
                .addToLabels("kubernetes.io/cluster-service", "true")
                .addToLabels("addonmanager.kubernetes.io/mode", "Reconcile")
                .endMetadata().build();
        client.serviceAccounts().inNamespace(namespace).createOrReplace(sa);
        //创建rolebinding
        ClusterRoleBinding clusterRoleBinding = client.rbac().clusterRoleBindings().withName("crux-cisupport-container-terminal").get();
        if(null == clusterRoleBinding){
            /**
             * kind: ClusterRoleBinding
             * apiVersion: rbac.authorization.k8s.io/v1beta1
             * metadata:
             *   name: admin
             *   annotations:
             *     rbac.authorization.kubernetes.io/autoupdate: "true"
             * roleRef:
             *   kind: ClusterRole
             *   name: cluster-admin
             *   apiGroup: rbac.authorization.k8s.io
             * subjects:
             * - kind: ServiceAccount
             *   name: admin
             *   namespace: kube-system
             * ---
             * apiVersion: v1
             * kind: ServiceAccount
             * metadata:
             *   name: admin
             *   namespace: kube-system
             *   labels:
             *     kubernetes.io/cluster-service: "true"
             *     addonmanager.kubernetes.io/mode: Reconcile
             */
            clusterRoleBinding = new ClusterRoleBindingBuilder()
                    .editOrNewMetadata()
                    .withName("crux-cisupport-container-terminal")
                    .addToAnnotations("rbac.authorization.kubernetes.io/autoupdate", "true")
                    .endMetadata()
                    .withRoleRef(new RoleRefBuilder().withKind("ClusterRole").withName("cluster-admin").withApiGroup("rbac.authorization.k8s.io").build())
                    .addToSubjects(new SubjectBuilder().withKind("ServiceAccount").withName("crux-cisupport-container-terminal").withNamespace(namespace).build())
                    .build();
            client.rbac().clusterRoleBindings().create(clusterRoleBinding);
        }
    }
}
