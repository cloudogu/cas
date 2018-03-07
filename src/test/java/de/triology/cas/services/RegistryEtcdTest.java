package de.triology.cas.services;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatchers;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RegistryEtcd}.
 */
public class RegistryEtcdTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(
            WireMockConfiguration.options()
                    .dynamicPort()
                    .notifier(new ConsoleNotifier(false))
    );

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void createInstance() {
        RegistryEtcd registry = new RegistryEtcd();
        assertNotNull(registry);
    }

    @Test
    public void createInstanceWithNodeMasterFile() throws IOException {
        File file = temporaryFolder.newFile();
        Files.write("localhost", file, Charsets.UTF_8);

        RegistryEtcd registry = new RegistryEtcd(file.getAbsolutePath());
        assertNotNull(registry);
    }

    @Test
    public void getFqdn() {
        RegistryEtcd registry = createRegistry();
        assertEquals("ces.cloudogu.local", registry.getFqdn());
    }

    @Test
    public void getDogus() {
        RegistryEtcd registry = createRegistry();
        assertThat(registry.getDogus(), containsInAnyOrder("usermgt", "jenkins", "scm", "smeagol", "redmine"));
    }

    private RegistryEtcd createRegistry() {
        URI uri = URI.create("http://localhost:" + wireMockRule.port());
        return new RegistryEtcd(uri);
    }

    @Test
    public void getCorrectCasLogoutUri() throws ParseException, GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);
        JSONObject properties = new JSONObject();
        properties.put("logoutUri", "testDogu/logout");
        JSONObject doguMetaData = new JSONObject();
        doguMetaData.put("Properties", properties);
        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();

        URI logoutURI = registry.getCasLogoutUri("testDogu");
        assertEquals("testDogu/logout", logoutURI.toString());
    }

    @Test(expected = GetCasLogoutUriException.class)
    public void getCasLogoutUriFromNonexistentDogu() throws GetDoguNodeFromEtcdException, GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);

        when(registry.getDoguNodeFromEtcd("NonexistentDogu")).thenThrow(GetCasLogoutUriException.class);
        when(registry.getCasLogoutUri("NonexistentDogu")).thenCallRealMethod();

        registry.getCasLogoutUri("NonexistentDogu");
    }

    @Test(expected = GetCasLogoutUriException.class)
    public void getCasLogoutUriFromDoguWithoutProperties() throws ParseException, GetDoguNodeFromEtcdException, GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);
        JSONObject doguMetaData = new JSONObject();
        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();

        registry.getCasLogoutUri("testDogu");
    }

    @Test(expected = GetCasLogoutUriException.class)
    public void getCasLogoutUriFromDoguWithmalformedProperties() throws ParseException, GetDoguNodeFromEtcdException, GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);
        JSONObject doguMetaData = new JSONObject();
        doguMetaData.put("Properties", "malformedPropertiesData");

        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();

        registry.getCasLogoutUri("testDogu");
    }

    @Test(expected = GetCasLogoutUriException.class)
    public void getCasLogoutUriFromDoguWithoutLogoutUriInProperties() throws ParseException, GetDoguNodeFromEtcdException, GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);
        JSONObject properties = new JSONObject();
        JSONObject doguMetaData = new JSONObject();
        doguMetaData.put("Properties", properties);
        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();

        registry.getCasLogoutUri("testDogu");
    }

    @Test(expected = GetCasLogoutUriException.class)
    public void getCasLogoutUriFromDoguWithEmptyLogoutUriInProperties() throws ParseException, GetDoguNodeFromEtcdException, GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);
        JSONObject properties = new JSONObject();
        properties.put("logoutUri", null);
        JSONObject doguMetaData = new JSONObject();
        doguMetaData.put("Properties", properties);
        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();

        registry.getCasLogoutUri("testDogu");
    }

}