package de.triology.cas.services;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import de.triology.cas.services.dogu.CesDoguServiceFactory;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.requests.EtcdKeyGetRequest;
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


    @Rule
    public WireMockRule wireMockRule = new WireMockRule(
            WireMockConfiguration.options()
                    .dynamicPort()
                    .notifier(new ConsoleNotifier(false))
    );

    @Test
    public void getFqdn() {
        RegistryEtcd registry = createRegistry();
        assertEquals("ces.cloudogu.local", registry.getFqdn());
    }

    @Test
    public void getDogus() {
        RegistryEtcd registry = createRegistry();
        CesDoguServiceFactory factory = new CesDoguServiceFactory();
        List<String> installedDogus = registry.getInstalledDogusWhichAreUsingCAS(factory)
                .stream().map(CesServiceData::getName).collect(Collectors.toList());
        assertThat(installedDogus, containsInAnyOrder("nexus", "usermgt", "cockpit"));
        assertEquals(3, registry.getInstalledDogusWhichAreUsingCAS(factory).size());
    }

    private RegistryEtcd createRegistry() {
        URI uri = URI.create("http://localhost:" + wireMockRule.port());
        return new RegistryEtcd(new EtcdClientFactory().createEtcdClient(uri));
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

        registry.addDoguChangeListener(()-> {
            synchronized (dogus) {
                if (dogus.contains("dogu")) {
                    try {
                        when(request.send()).thenThrow(new IOException("second call"));
                    } catch (IOException e) {
                        e.printStackTrace();
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

    @Test(expected = GetCasLogoutUriException.class)
    public void getCasLogoutUriFromNonexistentDogu() throws GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);

        when(registry.getCasLogoutUri("NonexistentDogu")).thenCallRealMethod();

        registry.getCasLogoutUri("NonexistentDogu");
    }

    @Test(expected = GetCasLogoutUriException.class)
    public void getCasLogoutUriFromDoguWithoutProperties() throws ParseException, GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);
        JSONObject doguMetaData = new JSONObject();
        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();

        registry.getCasLogoutUri("testDogu");
    }

    @Test(expected = GetCasLogoutUriException.class)
    public void getCasLogoutUriFromDoguWithmalformedProperties() throws ParseException, GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);
        JSONObject doguMetaData = new JSONObject();
        doguMetaData.put("Properties", "malformedPropertiesData");

        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();

        registry.getCasLogoutUri("testDogu");
    }

    @Test(expected = GetCasLogoutUriException.class)
    public void getCasLogoutUriFromDoguWithoutLogoutUriInProperties() throws ParseException, GetCasLogoutUriException {
        RegistryEtcd registry = mock(RegistryEtcd.class);
        JSONObject properties = new JSONObject();
        JSONObject doguMetaData = new JSONObject();
        doguMetaData.put("Properties", properties);
        when(registry.getCurrentDoguNode(ArgumentMatchers.any())).thenReturn(doguMetaData);
        when(registry.getCasLogoutUri("testDogu")).thenCallRealMethod();

        registry.getCasLogoutUri("testDogu");
    }

    @Test(expected = GetCasLogoutUriException.class)
    public void getCasLogoutUriFromDoguWithEmptyLogoutUriInProperties() throws ParseException, GetCasLogoutUriException {
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