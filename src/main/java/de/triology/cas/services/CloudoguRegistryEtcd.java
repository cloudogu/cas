package de.triology.cas.services;

import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * Default implementation of {@link CloudoguRegistry} using {@link EtcdClient}.
 */
public class CloudoguRegistryEtcd implements CloudoguRegistry {
    private EtcdClient etcd;

    /**
     * Creates a cloudogu etcd client that loads its URI from <code>/etc/ces/node_master</code>.
     *
     * @throws CloudoguRegistryException when the URI cannot be read
     */
    public CloudoguRegistryEtcd() {
        try {
            etcd = new EtcdClient(URI.create(EtcdRegistryUtils.getEtcdUri()));
        } catch (IOException e) {
            throw new CloudoguRegistryException(e);
        }
    }

    @Override
    public List<String> getDogus() {
        try {
            List<EtcdKeysResponse.EtcdNode> nodes = etcd.getDir("/dogu").recursive().send().get().getNode().getNodes();
            return EtcdRegistryUtils.convertNodesToStringList(nodes);
        } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
            throw new CloudoguRegistryException(e);
        }
    }

    @Override
    public String getFqdn() {
        try {
            return etcd.get("/config/_global/fqdn").send().get().getNode().getValue();
        } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
            throw new CloudoguRegistryException(e);
        }
    }

    @Override
    public void addDoguChangeListener(DoguChangeListener doguChangeListener) {
        try {
            EtcdResponsePromise responsePromise = etcd.getDir("/dogu").recursive().waitForChange().send();
            responsePromise.addListener(promise -> {
                doguChangeListener.onChange();
            });
        } catch (IOException e) {
            throw new CloudoguRegistryException(e);
        }
    }
}
