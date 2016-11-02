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
 * Default implementation of {@link Registry} using {@link EtcdClient}.
 *
 * The Dogus are queried from etcd: Installed Dogus and the version iformation are stored in a directory
 * <code>/dogu/${name of dogu}/current</code>. In addition, 'cas' has to be in the dependencies of the Dogu.
 * Changes of the '/dogu' directory can be recognized using {@link #addDoguChangeListener(DoguChangeListener)}.
 */
class RegistryEtcd implements Registry {
    private EtcdClient etcd;

    /**
     * Creates a etcd client that loads its URI from <code>/etc/ces/node_master</code>.
     *
     * @throws RegistryException when the URI cannot be read
     */
    public RegistryEtcd() {
        try {
            // TODO when is this resource closed? Can spring be used to call etcd.close()?
            etcd = new EtcdClient(URI.create(EtcdRegistryUtils.getEtcdUri()));
        } catch (IOException e) {
            throw new RegistryException(e);
        }
    }

    @Override
    public List<String> getDogus() {
        try {
            List<EtcdKeysResponse.EtcdNode> nodes = etcd.getDir("/dogu").recursive().send().get().getNode().getNodes();
            return EtcdRegistryUtils.convertNodesToStringList(nodes);
        } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
            throw new RegistryException(e);
        }
    }

    @Override
    public String getFqdn() {
        try {
            return etcd.get("/config/_global/fqdn").send().get().getNode().getValue();
        } catch (IOException | EtcdException | EtcdAuthenticationException | TimeoutException e) {
            throw new RegistryException(e);
        }
    }

    @Override
    public void addDoguChangeListener(DoguChangeListener doguChangeListener) {
        try {
            EtcdResponsePromise responsePromise = etcd.getDir("/dogu").recursive().waitForChange().send();
            responsePromise.addListener(promise -> {
                doguChangeListener.onChange();
                /* Register again! Why?
                 * The promise is like some kind of long polling. Once the promise is fulfilled, the connection is
                 * gone. The listener, however, expects to be called on all following  changes, so just get a new
                 * promise. */
                addDoguChangeListener(doguChangeListener);
            });
        } catch (IOException e) {
            throw new RegistryException(e);
        }
    }
}
