package de.triology.cas.services;

import de.triology.cas.oidc.services.CesOAuthServiceFactory;
import de.triology.cas.services.dogu.CesDoguServiceFactory;
import org.apereo.cas.services.OidcRegisteredService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.hamcrest.collection.IsMapWithSize.anEmptyMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class RegistryLocalTest {

    @Rule
    public ExpectedException exceptionGrabber = ExpectedException.none();

    private static ByteArrayInputStream getServiceAccountYamlStream() {
        var localConfigYaml = """
                service_accounts:
                    cas:
                        usermgt:
                            created: "true"
                        redmine:
                            created: "true"
                    oidc:
                        teamscale:
                            secret: "teamscale_secret"
                        openproject:
                            secret: "openproject_secret"
                    oauth:
                        portainer:
                            secret: "portainer_secret"
                        some_oauth_dogu:
                            secret: "some_oauth_dogu_secret"
                """;
        return new ByteArrayInputStream(localConfigYaml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void getInstalledCasServiceAccountsOfTypeFailsWhenFileNotFound() {
        var registry = mock(RegistryLocal.class);
        var serviceFactory = new CesDoguServiceFactory();
        when(registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory)).thenCallRealMethod();
        when(registry.readServiceAccounts()).thenCallRealMethod();
        when(registry.getInputStreamForFile("/var/ces/config/local.yaml")).thenCallRealMethod();

        exceptionGrabber.expect(RegistryException.class);
        exceptionGrabber.expectMessage("Could not find file /var/ces/config/local.yaml");
        exceptionGrabber.expectCause(isA(FileNotFoundException.class));

        registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory);
    }

    @Test
    public void getInstalledCasServiceAccountsOfTypeWithInvalidYaml() {
        var registry = mock(RegistryLocal.class);
        var serviceFactory = new CesDoguServiceFactory();
        var yamlStream = new ByteArrayInputStream("invalid".getBytes(StandardCharsets.UTF_8));
        when(registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory)).thenCallRealMethod();
        when(registry.readServiceAccounts()).thenCallRealMethod();
        when(registry.getInputStreamForFile("/var/ces/config/local.yaml")).thenReturn(yamlStream);

        exceptionGrabber.expect(RegistryException.class);
        exceptionGrabber.expectMessage("Failed to parse yaml stream to class de.triology.cas.services.RegistryLocal$LocalConfig");
        exceptionGrabber.expectCause(isA(YAMLException.class));

        registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory);
    }

    @Test
    public void getInstalledCasServiceAccountsOfTypeWithNullYaml() {
        var registry = mock(RegistryLocal.class);
        var serviceFactory = new CesDoguServiceFactory();
        var yamlStream = new ByteArrayInputStream("null".getBytes(StandardCharsets.UTF_8));
        when(registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory)).thenCallRealMethod();
        when(registry.readServiceAccounts()).thenCallRealMethod();
        when(registry.getInputStreamForFile("/var/ces/config/local.yaml")).thenReturn(yamlStream);

        var result = registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory);
        assertThat(result, is(empty()));
    }

    @Test
    public void getInstalledCasServiceAccountsOfTypeFailsWithUnknownType() {
        var registry = mock(RegistryLocal.class);
        var serviceFactory = new CesDoguServiceFactory();
        var yamlStream = new ByteArrayInputStream("null".getBytes(StandardCharsets.UTF_8));
        when(registry.getInstalledCasServiceAccountsOfType("other", serviceFactory)).thenCallRealMethod();
        when(registry.readServiceAccounts()).thenCallRealMethod();
        when(registry.getInputStreamForFile("/var/ces/config/local.yaml")).thenReturn(yamlStream);

        exceptionGrabber.expect(RegistryException.class);
        exceptionGrabber.expectMessage("Unknown service account type other");

        registry.getInstalledCasServiceAccountsOfType("other", serviceFactory);
    }

    @Test
    public void getInstalledCasServiceAccountsOfTypeShouldFailToCloseInputStream() throws IOException {
        var registry = mock(RegistryLocal.class);
        var serviceFactory = new CesDoguServiceFactory();
        var yamlStream = spy(new ByteArrayInputStream("null".getBytes(StandardCharsets.UTF_8)));
        doThrow(IOException.class).when(yamlStream).close();
        when(registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory)).thenCallRealMethod();
        when(registry.readServiceAccounts()).thenCallRealMethod();
        when(registry.getInputStreamForFile("/var/ces/config/local.yaml")).thenReturn(yamlStream);

        exceptionGrabber.expect(RegistryException.class);
        exceptionGrabber.expectMessage("Failed to close local config file after reading service accounts.");
        exceptionGrabber.expectCause(isA(IOException.class));

        registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory);
    }

    @Test
    public void getInstalledCasServiceAccountsOfTypeWithEmptyYaml() {
        var registry = mock(RegistryLocal.class);
        var serviceFactory = new CesDoguServiceFactory();
        var yamlStream = new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8));
        when(registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory)).thenCallRealMethod();
        when(registry.readServiceAccounts()).thenCallRealMethod();
        when(registry.getInputStreamForFile("/var/ces/config/local.yaml")).thenReturn(yamlStream);

        var result = registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory);
        assertThat(result, is(empty()));
    }

    @Test
    public void getInstalledCasServiceAccountsOfTypeWithNullServiceAccounts() {
        var registry = mock(RegistryLocal.class);
        var serviceFactory = new CesDoguServiceFactory();
        var yamlStream = new ByteArrayInputStream("service_accounts: null".getBytes(StandardCharsets.UTF_8));
        when(registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory)).thenCallRealMethod();
        when(registry.readServiceAccounts()).thenCallRealMethod();
        when(registry.getInputStreamForFile("/var/ces/config/local.yaml")).thenReturn(yamlStream);

        var result = registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory);
        assertThat(result, is(empty()));
    }

    @Test
    public void getInstalledCasServiceAccountsOfTypeWithEmptyServiceAccounts() {
        var registry = mock(RegistryLocal.class);
        var serviceFactory = new CesDoguServiceFactory();
        var yamlStream = new ByteArrayInputStream("service_accounts: {}".getBytes(StandardCharsets.UTF_8));
        when(registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory)).thenCallRealMethod();
        when(registry.readServiceAccounts()).thenCallRealMethod();
        when(registry.getInputStreamForFile("/var/ces/config/local.yaml")).thenReturn(yamlStream);

        var result = registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory);
        assertThat(result, is(empty()));
    }

    @Test
    public void getInstalledCasServiceAccountsOfTypeWithNullServiceAccountsInner() {
        var registry = mock(RegistryLocal.class);
        var serviceFactory = new CesDoguServiceFactory();
        var localConfigYaml = """
                service_accounts:
                    cas: null
                    oidc: null
                    oauth: null
                """;
        var yamlStream = new ByteArrayInputStream(localConfigYaml.getBytes(StandardCharsets.UTF_8));
        when(registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory)).thenCallRealMethod();
        when(registry.readServiceAccounts()).thenCallRealMethod();
        when(registry.getInputStreamForFile("/var/ces/config/local.yaml")).thenReturn(yamlStream);

        var result = registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory);
        assertThat(result, is(empty()));
    }

    @Test
    public void getInstalledCasServiceAccountsOfTypeWithEmptyServiceAccountsInner() {
        var registry = mock(RegistryLocal.class);
        var serviceFactory = new CesDoguServiceFactory();
        var localConfigYaml = """
                service_accounts:
                    cas: {}
                    oidc: {}
                    oauth: {}
                """;
        var yamlStream = new ByteArrayInputStream(localConfigYaml.getBytes(StandardCharsets.UTF_8));
        when(registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory)).thenCallRealMethod();
        when(registry.readServiceAccounts()).thenCallRealMethod();
        when(registry.getInputStreamForFile("/var/ces/config/local.yaml")).thenReturn(yamlStream);

        var result = registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory);
        assertThat(result, is(empty()));
    }

    @Test
    public void getInstalledCasServiceAccountsOfTypeCas() {
        var registry = mock(RegistryLocal.class);
        var serviceFactory = new CesDoguServiceFactory();
        var yamlStream = getServiceAccountYamlStream();
        when(registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory)).thenCallRealMethod();
        when(registry.readServiceAccounts()).thenCallRealMethod();
        when(registry.getInputStreamForFile("/var/ces/config/local.yaml")).thenReturn(yamlStream);

        var result = registry.getInstalledCasServiceAccountsOfType("cas", serviceFactory);
        assertThat(result, hasSize(2));
        assertThat(result, hasItem(allOf(
                hasProperty("name", equalTo("usermgt")),
                hasProperty("factory", equalTo(serviceFactory)),
                hasProperty("attributes", is(anEmptyMap()))
        )));
        assertThat(result, hasItem(allOf(
                hasProperty("name", equalTo("redmine")),
                hasProperty("factory", equalTo(serviceFactory)),
                hasProperty("attributes", is(anEmptyMap()))
        )));
    }

    @Test
    public void getInstalledCasServiceAccountsOfTypeOidc() {
        var registry = mock(RegistryLocal.class);
        var serviceFactory = new CesOAuthServiceFactory<>(OidcRegisteredService::new);
        var yamlStream = getServiceAccountYamlStream();
        when(registry.getInstalledCasServiceAccountsOfType("oidc", serviceFactory)).thenCallRealMethod();
        when(registry.readServiceAccounts()).thenCallRealMethod();
        when(registry.getInputStreamForFile("/var/ces/config/local.yaml")).thenReturn(yamlStream);

        var result = registry.getInstalledCasServiceAccountsOfType("oidc", serviceFactory);
        assertThat(result, hasSize(2));
        assertThat(result, hasItem(allOf(
                hasProperty("name", equalTo("teamscale")),
                hasProperty("factory", equalTo(serviceFactory)),
                hasProperty("attributes", allOf(
                        is(aMapWithSize(2)),
                        hasEntry("oauth_client_id", "teamscale"),
                        hasEntry("oauth_client_secret", "teamscale_secret")
                ))
        )));
        assertThat(result, hasItem(allOf(
                hasProperty("name", equalTo("openproject")),
                hasProperty("factory", equalTo(serviceFactory)),
                hasProperty("attributes", allOf(
                        is(aMapWithSize(2)),
                        hasEntry("oauth_client_id", "openproject"),
                        hasEntry("oauth_client_secret", "openproject_secret")
                ))
        )));
    }

    @Test
    public void getInstalledCasServiceAccountsOfTypeOAuth() {
        var registry = mock(RegistryLocal.class);
        var serviceFactory = new CesOAuthServiceFactory<>(OidcRegisteredService::new);
        var yamlStream = getServiceAccountYamlStream();
        when(registry.getInstalledCasServiceAccountsOfType("oauth", serviceFactory)).thenCallRealMethod();
        when(registry.readServiceAccounts()).thenCallRealMethod();
        when(registry.getInputStreamForFile("/var/ces/config/local.yaml")).thenReturn(yamlStream);

        var result = registry.getInstalledCasServiceAccountsOfType("oauth", serviceFactory);
        assertThat(result, hasSize(2));
        assertThat(result, hasItem(allOf(
                hasProperty("name", equalTo("portainer")),
                hasProperty("factory", equalTo(serviceFactory)),
                hasProperty("attributes", allOf(
                        is(aMapWithSize(2)),
                        hasEntry("oauth_client_id", "portainer"),
                        hasEntry("oauth_client_secret", "portainer_secret")
                ))
        )));
        assertThat(result, hasItem(allOf(
                hasProperty("name", equalTo("some_oauth_dogu")),
                hasProperty("factory", equalTo(serviceFactory)),
                hasProperty("attributes", allOf(
                        is(aMapWithSize(2)),
                        hasEntry("oauth_client_id", "some_oauth_dogu"),
                        hasEntry("oauth_client_secret", "some_oauth_dogu_secret")
                ))
        )));
    }

    @Test
    public void getCasLogoutUriFailsForNonExistentDogu() throws GetCasLogoutUriException {
        var registry = spy(RegistryLocal.class);
        var localConfigYaml = """
                service_accounts:
                    cas:
                        usermgt:
                            created: "true"
                            logout_uri: "/var/ces/config/local.yaml"
                """;
        var yamlStream = new ByteArrayInputStream(localConfigYaml.getBytes(StandardCharsets.UTF_8));
        doReturn(yamlStream).when(registry).getInputStreamForFile("/var/ces/config/local.yaml");

        exceptionGrabber.expect(GetCasLogoutUriException.class);
        exceptionGrabber.expectMessage("Could not get logoutUri for dogu my_dogu");

        registry.getCasLogoutUri("my_dogu");
    }

    @Test
    public void getCasLogoutUriFailsForEmptyOrNullUri() throws GetCasLogoutUriException {
        var registry = spy(RegistryLocal.class);
        var localConfigYaml = """
                service_accounts:
                    cas:
                        my_dogu:
                            created: "true"
                            logout_uri: ""
                    oidc:
                        my_dogu:
                            secret: "my_secret"
                            logout_uri: null
                """;
        var yamlStream = new ByteArrayInputStream(localConfigYaml.getBytes(StandardCharsets.UTF_8));
        doReturn(yamlStream).when(registry).getInputStreamForFile("/var/ces/config/local.yaml");

        exceptionGrabber.expect(GetCasLogoutUriException.class);
        exceptionGrabber.expectMessage("Could not get logoutUri for dogu my_dogu");

        registry.getCasLogoutUri("my_dogu");
    }

    @Test
    public void getCasLogoutUriFailsForInvalidUri() throws GetCasLogoutUriException {
        var registry = spy(RegistryLocal.class);
        var localConfigYaml = """
                service_accounts:
                    cas:
                        my_dogu:
                            created: "true"
                            logout_uri: "<invalid>"
                """;
        var yamlStream = new ByteArrayInputStream(localConfigYaml.getBytes(StandardCharsets.UTF_8));
        doReturn(yamlStream).when(registry).getInputStreamForFile("/var/ces/config/local.yaml");

        exceptionGrabber.expect(GetCasLogoutUriException.class);
        exceptionGrabber.expectCause(isA(URISyntaxException.class));

        registry.getCasLogoutUri("my_dogu");
    }

    @Test
    public void getCasLogoutUriSuccess() throws GetCasLogoutUriException, URISyntaxException {
        var registry = spy(RegistryLocal.class);
        var localConfigYaml = """
                service_accounts:
                    cas:
                        my_dogu:
                            created: "true"
                            logout_uri: "/api/logout"
                """;
        var yamlStream = new ByteArrayInputStream(localConfigYaml.getBytes(StandardCharsets.UTF_8));
        doReturn(yamlStream).when(registry).getInputStreamForFile("/var/ces/config/local.yaml");

        var result = registry.getCasLogoutUri("my_dogu");
        assertThat(result, is(new URI("/api/logout")));
    }

    @Test
    public void getCasLogoutUriSuccessMultiple() throws GetCasLogoutUriException, URISyntaxException {
        var registry = spy(RegistryLocal.class);
        var localConfigYaml = """
                service_accounts:
                    cas:
                        my_dogu:
                            created: "true"
                            logout_uri: "/api/logout"
                    oidc:
                        my_dogu:
                            secret: "my_secret"
                            logout_uri: "/api/logout"
                    oauth:
                        my_dogu:
                            secret: "my_secret"
                            logout_uri: "/api/logout"
                """;
        var yamlStream = new ByteArrayInputStream(localConfigYaml.getBytes(StandardCharsets.UTF_8));
        doReturn(yamlStream).when(registry).getInputStreamForFile("/var/ces/config/local.yaml");

        var result = registry.getCasLogoutUri("my_dogu");
        assertThat(result, is(new URI("/api/logout")));
    }

    @Test
    public void getFqdnFromNullConfig() {
        var registry = spy(RegistryLocal.class);

        var yamlStream = new ByteArrayInputStream("null".getBytes(StandardCharsets.UTF_8));
        doReturn(yamlStream).when(registry).getInputStreamForFile("/etc/ces/config/global/config.yaml");

        var fqdn = registry.getFqdn();
        assertThat(fqdn, is(nullValue()));
    }

    @Test
    public void getFqdnWhenNull() {
        var registry = spy(RegistryLocal.class);

        var yamlStream = new ByteArrayInputStream("fqdn: null".getBytes(StandardCharsets.UTF_8));
        doReturn(yamlStream).when(registry).getInputStreamForFile("/etc/ces/config/global/config.yaml");

        var fqdn = registry.getFqdn();
        assertThat(fqdn, is(nullValue()));
    }

    @Test
    public void getFqdnWhenEmpty() {
        var registry = spy(RegistryLocal.class);

        var yamlStream = new ByteArrayInputStream("fqdn: \"\"".getBytes(StandardCharsets.UTF_8));
        doReturn(yamlStream).when(registry).getInputStreamForFile("/etc/ces/config/global/config.yaml");

        var fqdn = registry.getFqdn();
        assertThat(fqdn, isEmptyString());
    }

    @Test
    public void getFqdnSuccess() {
        var registry = spy(RegistryLocal.class);

        var yamlStream = new ByteArrayInputStream("fqdn: \"ces.example.com\"".getBytes(StandardCharsets.UTF_8));
        doReturn(yamlStream).when(registry).getInputStreamForFile("/etc/ces/config/global/config.yaml");

        var fqdn = registry.getFqdn();
        assertThat(fqdn, is("ces.example.com"));
    }

    @Test
    public void getFqdnFailToCloseStream() throws IOException {
        var yamlStream = spy(new ByteArrayInputStream("fqdn: \"ces.example.com\"".getBytes(StandardCharsets.UTF_8)));
        doThrow(IOException.class).when(yamlStream).close();

        var registry = spy(RegistryLocal.class);
        doReturn(yamlStream).when(registry).getInputStreamForFile("/etc/ces/config/global/config.yaml");

        exceptionGrabber.expect(RegistryException.class);
        exceptionGrabber.expectMessage("Failed to close global config file after reading fqdn.");
        exceptionGrabber.expectCause(isA(IOException.class));

        registry.getFqdn();
    }

    @Test
    public void ChangeListener() throws IOException, InterruptedException {
        var watchKey = mock(WatchKey.class);
        when(watchKey.isValid())
                .thenReturn(true);
        when(watchKey.pollEvents())
                .thenReturn(null); // The List of events is not interesting because we handle all the same way.
        when(watchKey.reset())
                .thenReturn(true);
        Class<? extends WatchService> wsClass;
        try (var ws = FileSystems.getDefault().newWatchService()) {
            wsClass = ws.getClass();
        }
        var watchService = mock(wsClass);
        when(watchService.take())
                .thenReturn(watchKey)
                .thenThrow(InterruptedException.class); // finish on fourth invocation
        var fileSystem = mock(FileSystem.class);
        when(fileSystem.newWatchService()).thenReturn(watchService);

        var registry = spy(RegistryLocal.class);
        registry.fileSystem = fileSystem;
        var initialServiceAccounts = new RegistryLocal.ServiceAccounts();
        var changedServiceAccounts = new RegistryLocal.ServiceAccounts();
        changedServiceAccounts.setCas(Map.of("usermgt", new RegistryLocal.ServiceAccountCas()));
        var unchangedServiceAccounts2 = new RegistryLocal.ServiceAccounts();
        unchangedServiceAccounts2.setCas(Map.of("usermgt", new RegistryLocal.ServiceAccountCas()));
        doReturn(initialServiceAccounts, // get initial state for comparison
                changedServiceAccounts, // detect change
                unchangedServiceAccounts2) // no change to previous state
                .when(registry).readServiceAccounts();

        ArrayList<String> dogus = new ArrayList<>();

        registry.addDoguChangeListener(() -> {
            synchronized (dogus) {
                dogus.add("dogu " + dogus.size());
                dogus.notify();
            }
        });

        synchronized (dogus) {
            dogus.wait();
        }

        assertThat(dogus, hasSize(1));
    }

    @Test
    public void ChangeListenerWithReInitialization() throws IOException, InterruptedException {
        var watchKey = mock(WatchKey.class);
        when(watchKey.isValid())
                .thenReturn(false)
                .thenReturn(true)
                .thenReturn(true);
        when(watchKey.pollEvents())
                .thenReturn(null); // The List of events is not interesting because we handle all the same way.
        when(watchKey.reset())
                .thenReturn(false)
                .thenReturn(true);
        Class<? extends WatchService> wsClass;
        try (var ws = FileSystems.getDefault().newWatchService()) {
            wsClass = ws.getClass();
        }
        var watchService = mock(wsClass);
        when(watchService.take())
                .thenReturn(watchKey)
                .thenReturn(watchKey)
                .thenReturn(watchKey)
                .thenThrow(InterruptedException.class); // finish on fourth invocation
        var fileSystem = mock(FileSystem.class);
        when(fileSystem.newWatchService())
                .thenReturn(watchService)
                .thenReturn(watchService)
                .thenReturn(watchService);

        var registry = spy(RegistryLocal.class);
        registry.fileSystem = fileSystem;
        var initialServiceAccounts = new RegistryLocal.ServiceAccounts();
        var changedServiceAccounts = new RegistryLocal.ServiceAccounts();
        changedServiceAccounts.setCas(Map.of("usermgt", new RegistryLocal.ServiceAccountCas()));
        var unchangedServiceAccounts2 = new RegistryLocal.ServiceAccounts();
        unchangedServiceAccounts2.setCas(Map.of("usermgt", new RegistryLocal.ServiceAccountCas()));
        doReturn(initialServiceAccounts, // get initial state for comparison
                initialServiceAccounts, // no change
                changedServiceAccounts, // detect change
                unchangedServiceAccounts2) // no change to previous state
                .when(registry).readServiceAccounts();

        ArrayList<String> dogus = new ArrayList<>();

        registry.addDoguChangeListener(() -> {
            synchronized (dogus) {
                dogus.add("dogu " + dogus.size());
                dogus.notify();
            }
        });

        synchronized (dogus) {
            dogus.wait();
        }

        assertThat(dogus, hasSize(1));
    }

    @Test
    public void deepEquals() {
        Map<String, RegistryLocal.ServiceAccountCas> cas = new HashMap<>();
        cas.put("usermgt", createServiceAccountCas("true", "/logout"));
        cas.put("scm", createServiceAccountCas("false", "/logout2"));
        Map<String, RegistryLocal.ServiceAccountSecret> oidc = new HashMap<>();
        oidc.put("teamscale", createServiceAccountSecret("abc", "/logout3"));
        oidc.put("cas-oidc-dogu", createServiceAccountSecret("supersecret", ""));
        Map<String, RegistryLocal.ServiceAccountSecret> oauth = new HashMap<>();
        oauth.put("portainer", createServiceAccountSecret("def", "/logout4"));
        oauth.put("scm", createServiceAccountSecret("notSecret", "/logout5"));

        RegistryLocal.ServiceAccounts serviceAccountsA = new RegistryLocal.ServiceAccounts();
        serviceAccountsA.setCas(cas);
        serviceAccountsA.setOidc(oidc);
        serviceAccountsA.setOauth(oauth);
        RegistryLocal.ServiceAccounts serviceAccountsB = new RegistryLocal.ServiceAccounts();
        serviceAccountsB.setCas(cas);
        serviceAccountsB.setOidc(oidc);
        serviceAccountsB.setOauth(oauth);

        assertTrue(serviceAccountsA.deepEquals(serviceAccountsB));
    }

    @Test
    public void deepEqualsFalseWhenCasDifferent() {
        Map<String, RegistryLocal.ServiceAccountCas> cas = new HashMap<>();
        cas.put("usermgt", createServiceAccountCas("true", "/logout"));
        cas.put("scm", createServiceAccountCas("false", "/logout2"));
        Map<String, RegistryLocal.ServiceAccountSecret> oidc = new HashMap<>();
        oidc.put("teamscale", createServiceAccountSecret("abc", "/logout3"));
        oidc.put("cas-oidc-dogu", createServiceAccountSecret("supersecret", ""));
        Map<String, RegistryLocal.ServiceAccountSecret> oauth = new HashMap<>();
        oauth.put("portainer", createServiceAccountSecret("def", "/logout4"));
        oauth.put("scm", createServiceAccountSecret("notSecret", "/logout5"));

        RegistryLocal.ServiceAccounts serviceAccountsA = new RegistryLocal.ServiceAccounts();
        serviceAccountsA.setCas(cas);
        serviceAccountsA.setOidc(oidc);
        serviceAccountsA.setOauth(oauth);

        RegistryLocal.ServiceAccounts serviceAccountsB = new RegistryLocal.ServiceAccounts();
        Map<String, RegistryLocal.ServiceAccountCas> casDifferent = new HashMap<>();
        casDifferent.put("usermgt", createServiceAccountCas("false", "/logout"));
        casDifferent.put("scm", createServiceAccountCas("false", "/logout2"));
        serviceAccountsB.setCas(casDifferent);
        serviceAccountsB.setOidc(oidc);
        serviceAccountsB.setOauth(oauth);

        assertFalse(serviceAccountsA.deepEquals(serviceAccountsB));
    }

    @Test
    public void deepEqualsFalseWhenOidcDifferent() {
        Map<String, RegistryLocal.ServiceAccountCas> cas = new HashMap<>();
        cas.put("usermgt", createServiceAccountCas("true", "/logout"));
        cas.put("scm", createServiceAccountCas("false", "/logout2"));
        Map<String, RegistryLocal.ServiceAccountSecret> oidc = new HashMap<>();
        oidc.put("teamscale", createServiceAccountSecret("abc", "/logout3"));
        oidc.put("cas-oidc-dogu", createServiceAccountSecret("supersecret", ""));
        Map<String, RegistryLocal.ServiceAccountSecret> oauth = new HashMap<>();
        oauth.put("portainer", createServiceAccountSecret("def", "/logout4"));
        oauth.put("scm", createServiceAccountSecret("notSecret", "/logout5"));

        RegistryLocal.ServiceAccounts serviceAccountsA = new RegistryLocal.ServiceAccounts();
        serviceAccountsA.setCas(cas);
        serviceAccountsA.setOidc(oidc);
        serviceAccountsA.setOauth(oauth);

        RegistryLocal.ServiceAccounts serviceAccountsB = new RegistryLocal.ServiceAccounts();
        serviceAccountsB.setCas(cas);
        Map<String, RegistryLocal.ServiceAccountSecret> oidcDifferent = new HashMap<>();
        oidc.put("teamscale", createServiceAccountSecret("abc", ""));
        oidc.put("cas-oidc-dogu", createServiceAccountSecret("supersecret", ""));
        serviceAccountsB.setOidc(oidcDifferent);
        serviceAccountsB.setOauth(oauth);

        assertFalse(serviceAccountsA.deepEquals(serviceAccountsB));
    }

    @Test
    public void deepEqualsFalseWhenOauthDifferent() {
        Map<String, RegistryLocal.ServiceAccountCas> cas = new HashMap<>();
        cas.put("usermgt", createServiceAccountCas("true", "/logout"));
        cas.put("scm", createServiceAccountCas("false", "/logout2"));
        Map<String, RegistryLocal.ServiceAccountSecret> oidc = new HashMap<>();
        oidc.put("teamscale", createServiceAccountSecret("abc", "/logout3"));
        oidc.put("cas-oidc-dogu", createServiceAccountSecret("supersecret", ""));
        Map<String, RegistryLocal.ServiceAccountSecret> oauth = new HashMap<>();
        oauth.put("portainer", createServiceAccountSecret("def", "/logout4"));
        oauth.put("scm", createServiceAccountSecret("notSecret", "/logout5"));

        RegistryLocal.ServiceAccounts serviceAccountsA = new RegistryLocal.ServiceAccounts();
        serviceAccountsA.setCas(cas);
        serviceAccountsA.setOidc(oidc);
        serviceAccountsA.setOauth(oauth);

        RegistryLocal.ServiceAccounts serviceAccountsB = new RegistryLocal.ServiceAccounts();
        serviceAccountsB.setCas(cas);
        serviceAccountsB.setOidc(oidc);
        Map<String, RegistryLocal.ServiceAccountSecret> oauthDifferent = new HashMap<>();
        oauthDifferent.put("portainerDifferent", createServiceAccountSecret("def", "/logout4"));
        oauthDifferent.put("scm", createServiceAccountSecret("notSecret", "/logout5"));
        serviceAccountsB.setOauth(oauthDifferent);

        assertFalse(serviceAccountsA.deepEquals(serviceAccountsB));
    }

    private RegistryLocal.ServiceAccountCas createServiceAccountCas(String created, String logout_uri) {
        RegistryLocal.ServiceAccountCas cas = new RegistryLocal.ServiceAccountCas();
        cas.setCreated(created);
        cas.setLogout_uri(logout_uri);
        return cas;
    }

    private RegistryLocal.ServiceAccountSecret createServiceAccountSecret(String secret, String logout_uri) {
        RegistryLocal.ServiceAccountSecret serviceAccount = new RegistryLocal.ServiceAccountSecret();
        serviceAccount.setSecret(secret);
        serviceAccount.setLogout_uri(logout_uri);
        return serviceAccount;
    }
}