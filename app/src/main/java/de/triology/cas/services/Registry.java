package de.triology.cas.services;

import de.triology.cas.services.dogu.CesServiceFactory;

import java.net.URI;
import java.util.List;

/**
 * Abstraction of a registry that provides service information.
 */
interface Registry {

    /**
     * @return an array of {@link CesServiceData} containing the information for all installed dogus
     */
    List<CesServiceData> getInstalledDogusWhichAreUsingCAS(CesServiceFactory factory);

    /**
     * Retrieves all CAS Services Accounts which are currently registered in etcd.
     *
     * @param factory            The factory responsible to create a service of the given type
     * @param serviceAccountType The type of service account that should be searched in the registry
     * @return an array of {@link CesServiceData} containing the information for all installed oauth service accounts
     * of the given type
     */
    List<CesServiceData> getInstalledCasServiceAccountsOfType(String serviceAccountType, CesServiceFactory factory);

    /**
     * @return the fully qualified domain name
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
