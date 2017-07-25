package de.triology.cas.services;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

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
  public void createInstance() {
    RegistryEtcd registry = new RegistryEtcd();
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

}