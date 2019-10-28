package de.triology.cas.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * This class provides links from the etcd registry for the bottom fragment of every view that uses it. If one link is
 * not set or unavailable no respective link should be seen.
 * <p>
 * The sequence of these links is tackled down to
 *     <ol>
 *         <li>Terms of Services</li>
 *         <li>Imprint</li>
 *         <li>Privacy Policy</li>
 *     </ol>
 * </p>
 */
public class LegalLinkProducer {
    private static final Logger LOG = LoggerFactory.getLogger(LegalLinkProducer.class);
    private static final String REGISTRY_KEY_TERMS_OF_SERVICE = "/config/cas/legal_urls/terms_of_service";
    private static final String REGISTRY_KEY_IMPRINT = "/config/cas/legal_urls/imprint";
    private static final String REGISTRY_KEY_PRIVACY_POLICY = "/config/cas/legal_urls/privacy_policy";
    private static final String LINK_DELIMITER = "|";
    private final RegistryEtcd registry;
    /**
     * This field provides a simple cache in order to reduce the strong etcd interaction latency.
     * <h1>Note on cache invalidation</h1>
     * This class does not provide any means of cache invalidation mechanism because these data are
     * supposed not to change frequently. Instead it is supposed to restart the CAS container after the etcd values are
     * correctly set.
     */
    final Map<String, String> etcdKeyToValueCache;

    LegalLinkProducer(RegistryEtcd registry) {
        this.registry = registry;
        this.etcdKeyToValueCache = new HashMap<>(3);
    }

    /**
     * returns a TOS link or the empty string.
     *
     * @return a TOS link or the empty string.
     */
    public String getTermsOfServiceLink() {
        return getEtcdValueOrEmpty(REGISTRY_KEY_TERMS_OF_SERVICE);
    }

    /**
     * returns a delimiter if another link is following or the empty string.
     *
     * @return a delimiter if another link is following or the empty string.
     */
    public String getTermsOfServiceLinkDelimiter() {
            boolean isLinkActive = !getTermsOfServiceLink().isEmpty();
            if (isLinkActive && hasFollowingEntry(REGISTRY_KEY_IMPRINT, REGISTRY_KEY_PRIVACY_POLICY)) {
                return LINK_DELIMITER;
            }

        return "";
    }

    /**
     * returns a imprint link or the empty string.
     *
     * @return a imprint link or the empty string.
     */
    public String getImprintLink() {
        return getEtcdValueOrEmpty(REGISTRY_KEY_IMPRINT);
    }

    /**
     * returns a delimiter if another link is following or the empty string.
     *
     * @return a delimiter if another link is following or the empty string.
     */
    public String getImprintLinkDelimiter() {
        boolean isLinkActive = !getImprintLink().isEmpty();
        if (isLinkActive && hasFollowingEntry(REGISTRY_KEY_PRIVACY_POLICY)) {
            return LINK_DELIMITER;
        }

        return "";
    }

    /**
     * returns a privacy policy link or the empty string.
     *
     * @return a privacy policy link or the empty string.
     */
    public String getPrivacyPolicyLink() {
        return getEtcdValueOrEmpty(REGISTRY_KEY_PRIVACY_POLICY);
    }

    private String getEtcdValueOrEmpty(String key) {
        boolean missesCache = etcdKeyToValueCache.get(key) == null;
        if (missesCache) {
            LOG.info("Getting etcd value for key {}", key);
            String value = "";
            try {
                value = registry.getEtcdValueForKey(key);
            } catch (Exception e) {
                LOG.warn("Could not access registry for key {}. This key will be ignored from now on. {}", key, e);
            }
            etcdKeyToValueCache.put(key, value);
        } else {
            LOG.debug("Taking etcd value for key {} from internal cache", key);
        }
        return etcdKeyToValueCache.get(key);
    }

    private boolean hasFollowingEntry(String... keys) {
        for (String key : keys) {
            String value = getEtcdValueOrEmpty(key);
            if (!value.isEmpty()) {
                return true;
            }
        }

        return false;
    }

}