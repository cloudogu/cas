package de.triology.cas.services;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import de.triology.cas.oidc.services.CesOAuthServiceFactory;
import de.triology.cas.services.dogu.CesDoguServiceFactory;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.requests.EtcdKeyGetRequest;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RegistryEtcd}.
 */
public class RegistryEtcdTest {

    private static final String OAUTH_CLIENT_PORTAINER_SECRET = "cdf022a1583367cf3fd6795be0eef0c8ce6f764143fcd9d851934750b0f4f39f";
    private static final String OIDC_CLIENT_CAS_OIDC_SECRET = "834251c84c1b88ce39351d888ee04df91e89785a28dbd86244e0e22c9d27b41f";
//
//    @Rule
//    public WireMockRule wireMockRule = new WireMockRule(
//            WireMockConfiguration.options()
//                    .dynamicPort()
//                    .notifier(new ConsoleNotifier(false))
//    );
//
//    @Test
//    public void getFqdn() {
//        RegistryEtcd registry = createRegistry();
//        assertEquals("192.168.56.2", registry.getFqdn());
//    }
//
//    @Test
//    public void getDogus() {
//        RegistryEtcd registry = createRegistry();
//        var factory = new CesDoguServiceFactory();
//        List<String> installedDogus = registry.getInstalledDogusWhichAreUsingCAS(factory)
//                .stream().map(CesServiceData::getName).toList();
//        assertTrue(installedDogus.contains("redmine"));
//        assertTrue(installedDogus.contains("usermgt"));
//        assertTrue(installedDogus.contains("nexus"));
//        assertTrue(installedDogus.contains("portainer"));
//        assertTrue(installedDogus.contains("scm"));
//        assertTrue(installedDogus.contains("cas-oidc-client"));
//        assertTrue(installedDogus.contains("cockpit"));
//        assertTrue(registry.getInstalledDogusWhichAreUsingCAS(factory).size() >= 7);
//    }
//
//    private RegistryEtcd createRegistry() {
//        URI uri = URI.create("http://localhost:" + wireMockRule.port());
//        return new RegistryEtcd(new EtcdClientFactory().createEtcdClient(uri));
//    }
//
//    @Test
//    public void getCorrectCasLogoutUri() throws ParseException, GetCasLogoutUriException {
//        RegistryEtcd registry = mock(RegistryEtcd.class);
//        JSONObject properties = new JSONObject();
//        properties.put("logoutUri", "testDogu/logout");
//        JSONObject doguMetaData = new JSONObject();
//        doguMetaData.put("Properties", properties);
//        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
//        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();
//
//        URI logoutURI = registry.getCasLogoutUri("testDogu");
//        assertEquals("testDogu/logout", logoutURI.toString());
//    }
//
//    @Test
//    public void addDoguChangeListener() throws InterruptedException, IOException {
//        EtcdClient client = mock(EtcdClient.class);
//        EtcdResponsePromise responsePromise = mock(EtcdResponsePromise.class);
//        EtcdKeyGetRequest request = mock(EtcdKeyGetRequest.class);
//        when(client.getDir(ArgumentMatchers.any())).thenReturn(request);
//        when(request.recursive()).thenReturn(request);
//        when(request.waitForChange()).thenReturn(request);
//        when(request.send()).thenReturn(responsePromise);
//        RegistryEtcd registry = new RegistryEtcd(client);
//        ArrayList<String> dogus = new ArrayList<>();
//
//        registry.addDoguChangeListener(() -> {
//            synchronized (dogus) {
//                if (dogus.contains("dogu")) {
//                    try {
//                        when(request.send()).thenThrow(new IOException("second call"));
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//                dogus.add("dogu");
//                dogus.notify();
//            }
//        });
//
//        synchronized (dogus) {
//            dogus.wait();
//        }
//        assertTrue(dogus.contains("dogu"));
//    }
//
//    @Test(expected = GetCasLogoutUriException.class)
//    public void getCasLogoutUriFromNonexistentDogu() throws GetCasLogoutUriException {
//        RegistryEtcd registry = mock(RegistryEtcd.class);
//
//        when(registry.getCasLogoutUri("NonexistentDogu")).thenCallRealMethod();
//
//        registry.getCasLogoutUri("NonexistentDogu");
//    }
//
//    @Test(expected = GetCasLogoutUriException.class)
//    public void getCasLogoutUriFromDoguWithoutProperties() throws ParseException, GetCasLogoutUriException {
//        RegistryEtcd registry = mock(RegistryEtcd.class);
//        JSONObject doguMetaData = new JSONObject();
//        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
//        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();
//
//        registry.getCasLogoutUri("testDogu");
//    }
//
//    @Test(expected = GetCasLogoutUriException.class)
//    public void getCasLogoutUriFromDoguWithmalformedProperties() throws ParseException, GetCasLogoutUriException {
//        RegistryEtcd registry = mock(RegistryEtcd.class);
//        JSONObject doguMetaData = new JSONObject();
//        doguMetaData.put("Properties", "malformedPropertiesData");
//
//        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
//        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();
//
//        registry.getCasLogoutUri("testDogu");
//    }
//
//    @Test(expected = GetCasLogoutUriException.class)
//    public void getCasLogoutUriFromDoguWithoutLogoutUriInProperties() throws ParseException, GetCasLogoutUriException {
//        RegistryEtcd registry = mock(RegistryEtcd.class);
//        JSONObject properties = new JSONObject();
//        JSONObject doguMetaData = new JSONObject();
//        doguMetaData.put("Properties", properties);
//        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
//        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();
//
//        registry.getCasLogoutUri("testDogu");
//    }
//
//    @Test(expected = GetCasLogoutUriException.class)
//    public void getCasLogoutUriFromDoguWithEmptyLogoutUriInProperties() throws ParseException, GetCasLogoutUriException {
//        RegistryEtcd registry = mock(RegistryEtcd.class);
//        JSONObject properties = new JSONObject();
//        properties.put("logoutUri", null);
//        JSONObject doguMetaData = new JSONObject();
//        doguMetaData.put("Properties", properties);
//        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
//        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();
//
//        registry.getCasLogoutUri("testDogu");
//    }
//
//
//    @Test
//    public void getOidcDogus() {
//        RegistryEtcd registry = createRegistry();
//        var factory = new CesOAuthServiceFactory<>(OidcRegisteredService::new);
//        List<String> installedServiceAccounts = registry.getInstalledCasServiceAccountsOfType(RegistryEtcd.SERVICE_ACCOUNT_TYPE_OIDC, factory)
//                .stream().map(CesServiceData::getName).collect(Collectors.toList());
//        assertThat(installedServiceAccounts, containsInAnyOrder("cas-oidc-client"));
//        assertEquals(1, registry.getInstalledCasServiceAccountsOfType(RegistryEtcd.SERVICE_ACCOUNT_TYPE_OIDC, factory).size());
//    }
//
//    @Test
//    public void getOidcDogus_CheckSecrets() {
//        RegistryEtcd registry = createRegistry();
//        var factory = new CesOAuthServiceFactory<>(OidcRegisteredService::new);
//        List<CesServiceData> installedServiceAccounts = registry.getInstalledCasServiceAccountsOfType(RegistryEtcd.SERVICE_ACCOUNT_TYPE_OAUTH, factory);
//        assertEquals(1, installedServiceAccounts.size());
//
//        installedServiceAccounts.stream().filter(e -> e.getName().equals("cas-oidc-client")).forEach(e -> {
//            assertEquals(OIDC_CLIENT_CAS_OIDC_SECRET, e.getAttributes().get(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH));
//            assertEquals("cas-oidc-client", e.getAttributes().get(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID));
//        });
//    }
//
//    @Test
//    public void getOAuthDogus() {
//        RegistryEtcd registry = createRegistry();
//        var factory = new CesOAuthServiceFactory<>(OAuthRegisteredService::new);
//        List<String> installedServiceAccounts = registry.getInstalledCasServiceAccountsOfType(RegistryEtcd.SERVICE_ACCOUNT_TYPE_OAUTH, factory)
//                .stream().map(CesServiceData::getName).collect(Collectors.toList());
//        assertThat(installedServiceAccounts, containsInAnyOrder("portainer"));
//        assertEquals(1, registry.getInstalledCasServiceAccountsOfType(RegistryEtcd.SERVICE_ACCOUNT_TYPE_OAUTH, factory).size());
//    }
//
//    @Test
//    public void getOAuthDogus_CheckSecrets() {
//        RegistryEtcd registry = createRegistry();
//        var factory = new CesOAuthServiceFactory<>(OAuthRegisteredService::new);
//        List<CesServiceData> installedServiceAccounts = registry.getInstalledCasServiceAccountsOfType(RegistryEtcd.SERVICE_ACCOUNT_TYPE_OAUTH, factory);
//        assertEquals(1, installedServiceAccounts.size());
//
//        installedServiceAccounts.stream().filter(e -> e.getName().equals("portainer")).forEach(e -> {
//            assertEquals(OAUTH_CLIENT_PORTAINER_SECRET, e.getAttributes().get(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH));
//            assertEquals("portainer", e.getAttributes().get(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID));
//        });
//    }
}
