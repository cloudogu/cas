package de.triology.cas.services;

import de.triology.cas.services.dogu.CesServiceFactory;

import java.net.URI;
import java.util.List;

/**
 * Abstraction of a registry that provides service information.
 */
public interface Registry {
    enum CasServiceAccountTypes {
        OAUTH("oauth"),
        OIDC("oidc"),
        CAS("cas"),
        UNDEFINED("");

        private final String id;

        CasServiceAccountTypes(String id) {
            this.id = id;
        }

        public static CasServiceAccountTypes fromString(String id) {
            for (CasServiceAccountTypes e : values()) {
                if (e.id.equals(id)) return e;
            }
            return UNDEFINED;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    String SERVICE_ACCOUNT_TYPE_OAUTH = "oauth";
    String SERVICE_ACCOUNT_TYPE_OIDC = "oidc";
    String SERVICE_ACCOUNT_TYPE_CAS = "cas";



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
