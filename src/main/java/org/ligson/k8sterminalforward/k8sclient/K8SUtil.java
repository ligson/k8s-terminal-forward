package org.ligson.k8sterminalforward.k8sclient;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

public class K8SUtil {
    public static void main(String[] args) {
        String k8sApiUrl = "https://rh.yonyougov.top/k8s/clusters/c-h5bcs";
        String userToken = "kubeconfig-user-26pfd.c-h5bcs:h4tgv5lr6pq8g6qwkst5vxbfs8vfncvp9gwx8bpbwsrlhnq8zgqt5k";
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

        KubernetesClient k8s = new KubernetesClientBuilder().withConfig(config).build();
        NamespaceList nsList = k8s.namespaces().list();
        for (Namespace item : nsList.getItems()) {
            System.out.println(item.getMetadata().getName());
        }
    }
}
