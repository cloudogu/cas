package de.triology.cas.services;

import java.util.List;

/**
 * Abstraction of a registry that provides service information.
 */
interface Registry {

    /**
     * @return the url of all dogus
     * @throws RegistryException wrapper for all technical exceptions
     */
    List<String> getDogus();

    /**
     * @return the fully qualified domain name
     * @throws RegistryException wrapper for all technical exceptions
     */
    String getFqdn();

    /**
     * Adds a listener that is called when a new dogu is added to or delted from etcd.
     *
     * @param doguChangeListener listener to be called on dogu change
     * @throws RegistryException wrapper for all technical exceptions
     */
    void addDoguChangeListener(DoguChangeListener doguChangeListener);

    /**
     * Functional interface for reacting on changes of dogus registerd in etcd
     */
    @FunctionalInterface
    interface DoguChangeListener {
        void onChange();
    }
}
