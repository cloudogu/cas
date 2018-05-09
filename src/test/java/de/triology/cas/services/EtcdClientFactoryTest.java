package de.triology.cas.services;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import mousio.etcd4j.EtcdClient;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatcher;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class EtcdClientFactoryTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldUseDefaultConfigFileForUrl() {
        EtcdClientFactory etcdClientFactory = spy(new EtcdClientFactory());
        EtcdClient etcdClient = etcdClientFactory.createEtcdClient();
        assertNotNull(etcdClient);
        verify(etcdClientFactory).createEtcdClient("/etc/ces/node_master");
    }

    @Test
    public void shouldReadExistingConfigFirForUrl() throws IOException {
        File file = temporaryFolder.newFile();
        Files.write("example.com", file, Charsets.UTF_8);

        EtcdClientFactory etcdClientFactory = spy(new EtcdClientFactory());
        EtcdClient etcdClient = etcdClientFactory.createEtcdClient(file.getAbsolutePath());
        assertNotNull(etcdClient);

        verify(etcdClientFactory).createEtcdClient(argThat(new URIMatcher("http://example.com:4001")));
    }

    @Test
    public void shouldFallbackToLocalhostIfConfigFileDoesNotExists() throws IOException {
        File file = temporaryFolder.newFolder();
        File notExistingFile = new File(file, "doesNotExist");

        EtcdClientFactory etcdClientFactory = spy(new EtcdClientFactory());
        EtcdClient etcdClient = etcdClientFactory.createEtcdClient(notExistingFile.getAbsolutePath());
        assertNotNull(etcdClient);

        verify(etcdClientFactory).createEtcdClient(argThat(new URIMatcher("http://localhost:4001")));
    }

    @Test(expected = RegistryException.class)
    public void shouldFailIfConfigFileIsEmpty() throws IOException {
        File file = temporaryFolder.newFile();

        EtcdClientFactory etcdClientFactory = spy(new EtcdClientFactory());
        etcdClientFactory.createEtcdClient(file.getAbsolutePath());
    }

    private static class URIMatcher implements ArgumentMatcher<URI> {

        private final String expectedUrl;

        private URIMatcher(String expectedUrl) {
            this.expectedUrl = expectedUrl;
        }

        @Override
        public boolean matches(URI uri) {
            return expectedUrl.equals(uri.toString());
        }
    }
}