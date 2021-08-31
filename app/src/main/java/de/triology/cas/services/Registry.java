package de.triology.cas.services;

import de.triology.cas.services.dogu.ICesServiceFactory;

import java.net.URI;
import java.util.List;

/**
 * Abstraction of a registry that provides service information.
 */
interface Registry {

    /**
     * @return the names of all installed dogus wich are using CAS without the namespace
     * @throws RegistryException wrapper for all technical exceptions
     */
    List<CesServiceData> getInstalledDogusWhichAreUsingCAS(ICesServiceFactory factory);

    /**
     * @return the names of all installed OAuth service accounts
     * @throws RegistryException wrapper for all technical exceptions
     */
    List<CesServiceData> getInstalledOAuthCASServiceAccounts(ICesServiceFactory factory);

    /**
     * @return the fully qualified domain name
     * @throws RegistryException wrapper for all technical exceptions
     */
    String getFqdn();

    /**
     * @return the dogu specific CAS logout URI
     * @throws GetCasLogoutUriException wrapper for all technical exceptions
     */
    URI getCasLogoutUri(String doguname) throws GetCasLogoutUriException;

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
