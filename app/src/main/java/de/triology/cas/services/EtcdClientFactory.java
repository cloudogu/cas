package de.triology.cas.services;

import mousio.etcd4j.EtcdClient;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

@Component
public class EtcdClientFactory {

    private static final String DEFAULT_NOTE_MASTER_FILE = "/etc/ces/node_master";

    @Bean
    public EtcdClient createEtcdClient() {
        return createEtcdClient(DEFAULT_NOTE_MASTER_FILE);
    }

    public EtcdClient createEtcdClient(String nodeMasterFilepath) {
        try {
            // TODO when is this resource closed? Can spring be used to call etcd.close()?
            return createEtcdClient(URI.create(getEtcdUri(nodeMasterFilepath)));
        } catch (IOException e) {
            throw new RegistryException("Cannot create etcd client: ", e);
        }
    }

    public EtcdClient createEtcdClient(URI uri) {
        return new EtcdClient(uri);
    }

    private String getEtcdUri(String nodeMasterFilePath) throws IOException {
        File nodeMasterFile = new File(nodeMasterFilePath);
        if (!nodeMasterFile.exists()) {
            return "http://localhost:4001";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(nodeMasterFile))) {
            String nodeMaster = reader.readLine();
            if (StringUtils.isBlank(nodeMaster)) {
                throw new IOException("failed to read " + nodeMasterFilePath + " file");
            }
            return "http://".concat(nodeMaster).concat(":4001");
        }
    }
}
