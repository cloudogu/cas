package de.triology.cas.services;

import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;

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

}