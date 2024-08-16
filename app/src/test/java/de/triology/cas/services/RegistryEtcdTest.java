package de.triology.cas.services;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import de.triology.cas.oidc.services.CesOAuthServiceFactory;
import de.triology.cas.services.dogu.CesDoguServiceFactory;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.requests.EtcdKeyGetRequest;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import org.apereo.cas.services.OidcRegisteredService;
import org.apereo.cas.support.oauth.services.OAuthRegisteredService;
import org.hamcrest.MatcherAssert;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RegistryEtcd}.
 */
public class RegistryEtcdTest {

    private static final String OAUTH_CLIENT_PORTAINER_SECRET = "cdf022a1583367cf3fd6795be0eef0c8ce6f764143fcd9d851934750b0f4f39f";
    private static final String OIDC_CLIENT_CAS_OIDC_SECRET = "834251c84c1b88ce39351d888ee04df91e89785a28dbd86244e0e22c9d27b41f";

    @Rule
    public ExpectedException exceptionGrabber = ExpectedException.none();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(
            WireMockConfiguration.options()
                    .dynamicPort()
                    .notifier(new ConsoleNotifier(false))
    );

    @Test
    public void getFqdn() {
        RegistryEtcd registry = createRegistry();
        assertEquals("192.168.56.2", registry.getFqdn());
    }

    @Test
    public void getDogusOfTypeCas() {
        RegistryEtcd registry = createRegistry();
        var factory = new CesDoguServiceFactory();
        List<String> installedDogus = registry.getInstalledCasServiceAccountsOfType(Registry.SERVICE_ACCOUNT_TYPE_CAS, factory)
                .stream().map(CesServiceData::getName).toList();
        assertTrue(installedDogus.contains("redmine"));
        assertTrue(installedDogus.contains("usermgt"));
        assertTrue(installedDogus.contains("nexus"));
        assertTrue(installedDogus.contains("portainer"));
        assertTrue(installedDogus.contains("scm"));
        assertTrue(installedDogus.contains("cas-oidc-client"));
        assertTrue(installedDogus.contains("cockpit"));
    }

    @Test
    public void getDogusOfTypeOAuth() {
        RegistryEtcd registry = createRegistry();
        var factory = new CesDoguServiceFactory();
        List<String> installedDogus = registry.getInstalledCasServiceAccountsOfType(Registry.SERVICE_ACCOUNT_TYPE_OAUTH, factory)
                .stream().map(CesServiceData::getName).toList();
        assertTrue(installedDogus.contains("portainer"));
    }

    @Test
    public void getDogusOfTypeOidc() {
        RegistryEtcd registry = createRegistry();
        var factory = new CesDoguServiceFactory();
        List<String> installedDogus = registry.getInstalledCasServiceAccountsOfType(Registry.SERVICE_ACCOUNT_TYPE_OIDC, factory)
                .stream().map(CesServiceData::getName).toList();
        assertTrue(installedDogus.contains("cas-oidc-client"));
    }

    @Test
    public void getInstalledCasServiceAccountsOfTypeFailsWhenEtcdError() throws EtcdAuthenticationException, IOException, EtcdException, TimeoutException {
        var registry = mock(RegistryEtcd.class);
        var serviceFactory = new CesDoguServiceFactory();

        when(registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory)).thenCallRealMethod();
        when(registry.getInstalledDogusWhichAreUsingCAS(serviceFactory)).thenThrow(EtcdException.class);

        exceptionGrabber.expect(RegistryException.class);
        exceptionGrabber.expectMessage("Failed to getInstalledCasServiceAccountsOfType: cas");
        exceptionGrabber.expectCause(isA(EtcdException.class));

        registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory);
    }

    @Test
    public void getInstalledCasServiceAccountsOfTypeFailsWhenFileNotFound() throws EtcdAuthenticationException, IOException, EtcdException, TimeoutException {
        var registry = mock(RegistryEtcd.class);
        var serviceFactory = new CesDoguServiceFactory();

        when(registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory)).thenCallRealMethod();
        when(registry.getInstalledDogusWhichAreUsingCAS(serviceFactory)).thenThrow(IOException.class);

        exceptionGrabber.expect(RegistryException.class);
        exceptionGrabber.expectMessage("Failed to getInstalledCasServiceAccountsOfType: cas");
        exceptionGrabber.expectCause(isA(IOException.class));

        registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory);
    }


    private RegistryEtcd createRegistry() {
        URI uri = URI.create("http://localhost:" + wireMockRule.port());
        return new RegistryEtcd(new EtcdClientFactory().createEtcdClient(uri));
    }

    @Test
    public void getCorrectCasLogoutUri() throws GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);
        when(registry.getEtcdValueForKey("/config/cas/service_accounts/cas/testDogu/logout_uri")).thenReturn("testDogu/logout");
        when(registry.getEtcdValueForKey("/config/cas/service_accounts/oidc/testDogu/logout_uri")).thenThrow(RegistryException.class);
        when(registry.getEtcdValueForKey("/config/cas/service_accounts/oauth/testDogu/logout_uri")).thenThrow(RegistryException.class);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();

        URI logoutURI = registry.getCasLogoutUri("testDogu");
        assertEquals("testDogu/logout", logoutURI.toString());
    }

    @Test
    public void getCorrectOidcLogoutUriTypeAccount() throws GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);
        when(registry.getEtcdValueForKey("/config/cas/service_accounts/cas/testDogu/logout_uri")).thenThrow(RegistryException.class);
        when(registry.getEtcdValueForKey("/config/cas/service_accounts/oidc/testDogu/logout_uri")).thenReturn("testDogu/logout");
        when(registry.getEtcdValueForKey("/config/cas/service_accounts/oauth/testDogu/logout_uri")).thenThrow(RegistryException.class);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();

        URI logoutURI = registry.getCasLogoutUri("testDogu");
        assertEquals("testDogu/logout", logoutURI.toString());
    }

    @Test
    public void getCorrectOauthLogoutUri() throws GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);
        when(registry.getEtcdValueForKey("/config/cas/service_accounts/cas/testDogu/logout_uri")).thenThrow(RegistryException.class);
        when(registry.getEtcdValueForKey("/config/cas/service_accounts/oidc/testDogu/logout_uri")).thenThrow(RegistryException.class);
        when(registry.getEtcdValueForKey("/config/cas/service_accounts/oauth/testDogu/logout_uri")).thenReturn("testDogu/logout");
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();

        URI logoutURI = registry.getCasLogoutUri("testDogu");
        assertEquals("testDogu/logout", logoutURI.toString());
    }

    @Test(expected = GetCasLogoutUriException.class)
    public void getCasLogoutUriFromDoguDescriptorFallbackThrowsParseException() throws GetCasLogoutUriException, ParseException {
        RegistryEtcd registry = mock(RegistryEtcd.class);

        when(registry.getEtcdValueForKey(ArgumentMatchers.any())).thenThrow(RegistryException.class);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();
        when(registry.getCurrentDoguNode("testDogu")).thenThrow(ParseException.class);

        registry.getCasLogoutUri("testDogu");
    }

    @Test
    public void getCasLogoutUriFromDoguDescriptorFallback() throws GetCasLogoutUriException, ParseException {
        RegistryEtcd registry = mock(RegistryEtcd.class);

        when(registry.getEtcdValueForKey(ArgumentMatchers.any())).thenThrow(RegistryException.class);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();
        JSONObject properties = new JSONObject();
        properties.put("logoutUri", "testDogu/logout");
        JSONObject doguMetaData = new JSONObject();
        doguMetaData.put("Properties", properties);
        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);

        URI logoutURI = registry.getCasLogoutUri("testDogu");
        assertEquals("testDogu/logout", logoutURI.toString());
    }

    @Test(expected = GetCasLogoutUriException.class)
    public void getCasLogoutUriFromDoguDescriptorFallbackWithoutProperties() throws ParseException, GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);

        when(registry.getEtcdValueForKey(ArgumentMatchers.any())).thenThrow(RegistryException.class);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();
        JSONObject doguMetaData = new JSONObject();
        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();

        registry.getCasLogoutUri("testDogu");
    }

    @Test(expected = GetCasLogoutUriException.class)
    public void getCasLogoutUriFromDoguDescriptorFallbackWithMalformedProperties() throws ParseException, GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);

        when(registry.getEtcdValueForKey(ArgumentMatchers.any())).thenThrow(RegistryException.class);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();
        JSONObject doguMetaData = new JSONObject();
        doguMetaData.put("Properties", "malformedPropertiesData");

        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();

        registry.getCasLogoutUri("testDogu");
    }

    @Test(expected = GetCasLogoutUriException.class)
    public void getCasLogoutUriFromDoguDescriptorFallbackWithoutLogoutUriInProperties() throws ParseException, GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);

        when(registry.getEtcdValueForKey(ArgumentMatchers.any())).thenThrow(RegistryException.class);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();
        JSONObject properties = new JSONObject();
        JSONObject doguMetaData = new JSONObject();
        doguMetaData.put("Properties", properties);
        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();

        registry.getCasLogoutUri("testDogu");
    }

    @Test(expected = GetCasLogoutUriException.class)
    public void getCasLogoutUriFromDoguDescriptorFallbackWithEmptyLogoutUriInProperties() throws ParseException, GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);

        when(registry.getEtcdValueForKey(ArgumentMatchers.any())).thenThrow(RegistryException.class);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();
        JSONObject properties = new JSONObject();
        properties.put("logoutUri", null);
        JSONObject doguMetaData = new JSONObject();
        doguMetaData.put("Properties", properties);
        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();

        registry.getCasLogoutUri("testDogu");
    }

    @Test(expected = GetCasLogoutUriException.class)
    public void getCasLogoutUriFromNonexistentDogu() throws GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);

        when(registry.getEtcdValueForKey(ArgumentMatchers.any())).thenThrow(RegistryException.class);
        when(registry.getCasLogoutUri("NonexistentDogu")).thenCallRealMethod();

        registry.getCasLogoutUri("NonexistentDogu");
    }

    @Test(expected = GetCasLogoutUriException.class)
    public void getEmptyCasLogoutUri() throws GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);
        when(registry.getEtcdValueForKey(ArgumentMatchers.any())).thenReturn("");
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();

        registry.getCasLogoutUri("testDogu");
    }

    @Test
    public void addDoguChangeListener() throws InterruptedException, IOException {
        EtcdClient client = mock(EtcdClient.class);
        EtcdResponsePromise responsePromise = mock(EtcdResponsePromise.class);
        EtcdKeyGetRequest request = mock(EtcdKeyGetRequest.class);
        when(client.getDir(ArgumentMatchers.any())).thenReturn(request);
        when(request.recursive()).thenReturn(request);
        when(request.waitForChange()).thenReturn(request);
        when(request.send()).thenReturn(responsePromise);
        RegistryEtcd registry = new RegistryEtcd(client);
        ArrayList<String> dogus = new ArrayList<>();

        registry.addDoguChangeListener(() -> {
            synchronized (dogus) {
                if (dogus.contains("dogu")) {
                    try {
                        when(request.send()).thenThrow(new IOException("second call"));
                    } catch (IOException ignore) {
                    }
                }
                dogus.add("dogu");
                dogus.notify();
            }
        });

        synchronized (dogus) {
            dogus.wait();
        }
        assertTrue(dogus.contains("dogu"));
    }


    @Test
    public void getOidcDogus() {
        RegistryEtcd registry = createRegistry();
        var factory = new CesOAuthServiceFactory<>(OidcRegisteredService::new);
        List<String> installedServiceAccounts = registry.getInstalledCasServiceAccountsOfType(Registry.SERVICE_ACCOUNT_TYPE_OIDC, factory)
                .stream().map(CesServiceData::getName).collect(Collectors.toList());
        MatcherAssert.assertThat(installedServiceAccounts, containsInAnyOrder("cas-oidc-client"));
        assertEquals(1, registry.getInstalledCasServiceAccountsOfType(Registry.SERVICE_ACCOUNT_TYPE_OIDC, factory).size());
    }

    @Test
    public void getOidcDogus_CheckSecrets() {
        RegistryEtcd registry = createRegistry();
        var factory = new CesOAuthServiceFactory<>(OidcRegisteredService::new);
        List<CesServiceData> installedServiceAccounts = registry.getInstalledCasServiceAccountsOfType(Registry.SERVICE_ACCOUNT_TYPE_OAUTH, factory);
        assertEquals(1, installedServiceAccounts.size());

        installedServiceAccounts.stream().filter(e -> e.getName().equals("cas-oidc-client")).forEach(e -> {
            assertEquals(OIDC_CLIENT_CAS_OIDC_SECRET, e.getAttributes().get(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH));
            assertEquals("cas-oidc-client", e.getAttributes().get(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID));
        });
    }

    @Test
    public void getOAuthDogus() {
        RegistryEtcd registry = createRegistry();
        var factory = new CesOAuthServiceFactory<>(OAuthRegisteredService::new);
        List<String> installedServiceAccounts = registry.getInstalledCasServiceAccountsOfType(Registry.SERVICE_ACCOUNT_TYPE_OAUTH, factory)
                .stream().map(CesServiceData::getName).collect(Collectors.toList());
        MatcherAssert.assertThat(installedServiceAccounts, containsInAnyOrder("portainer"));
        assertEquals(1, registry.getInstalledCasServiceAccountsOfType(Registry.SERVICE_ACCOUNT_TYPE_OAUTH, factory).size());
    }

    @Test
    public void getOAuthDogus_CheckSecrets() {
        RegistryEtcd registry = createRegistry();
        var factory = new CesOAuthServiceFactory<>(OAuthRegisteredService::new);
        List<CesServiceData> installedServiceAccounts = registry.getInstalledCasServiceAccountsOfType(Registry.SERVICE_ACCOUNT_TYPE_OAUTH, factory);
        assertEquals(1, installedServiceAccounts.size());

        installedServiceAccounts.stream().filter(e -> e.getName().equals("portainer")).forEach(e -> {
            assertEquals(OAUTH_CLIENT_PORTAINER_SECRET, e.getAttributes().get(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_SECRET_HASH));
            assertEquals("portainer", e.getAttributes().get(CesOAuthServiceFactory.ATTRIBUTE_KEY_OAUTH_CLIENT_ID));
        });
    }

    @Test
    public void getCurrentDoguNode_UserMgtDogu() throws ParseException {
        RegistryEtcd registry = createRegistry();
        JSONObject test = registry.getCurrentDoguNode("usermgt");
        assertEquals(test.get("DisplayName"), "User Management");
        assertEquals(test.get("Image"), "registry.cloudogu.com/official/usermgt");
    }
}
