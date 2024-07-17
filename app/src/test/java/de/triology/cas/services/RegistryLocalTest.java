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

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapWithSize.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class RegistryLocalTest {

    @Rule
    public ExpectedException exceptionGrabber = ExpectedException.none();

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
}