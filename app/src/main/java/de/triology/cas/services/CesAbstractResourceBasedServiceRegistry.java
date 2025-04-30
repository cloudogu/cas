package de.triology.cas.services;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.services.resource.AbstractResourceBasedServiceRegistry;
import org.apereo.cas.services.resource.DefaultRegisteredServiceResourceNamingStrategy;
import org.apereo.cas.util.serialization.StringSerializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.stream.Collectors;

import org.apereo.cas.services.ServiceRegistryListener;
import org.apereo.cas.services.replication.NoOpRegisteredServiceReplicationStrategy;
import org.apereo.cas.util.io.WatcherService;
import java.io.File;
import java.nio.file.Path;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import org.apache.commons.io.FileUtils;
import lombok.val;



@Slf4j
public class CesAbstractResourceBasedServiceRegistry extends AbstractResourceBasedServiceRegistry {

    private final Collection<StringSerializer<RegisteredService>> mySerializers;


    public CesAbstractResourceBasedServiceRegistry(final Path configDirectory,
    final StringSerializer<RegisteredService> serializer,
    final ConfigurableApplicationContext applicationContext,
    final Collection<ServiceRegistryListener> serviceRegistryListeners) {
        super(configDirectory,
        serializer,
        applicationContext,
        new NoOpRegisteredServiceReplicationStrategy(),
        new DefaultRegisteredServiceResourceNamingStrategy(),
        serviceRegistryListeners,
        WatcherService.noOp());

        this.mySerializers = List.of(serializer);
    }

    
    @Override
    public Collection<RegisteredService> load(final File file) {
        val fileName = file.getName();
        LOGGER.debug("Trying to load: {}", fileName);

        if (!file.canRead() || !file.exists() || file.length() == 0 || fileName.startsWith(".")) {
            LOGGER.debug("Skipping unreadable/empty/hidden file: {}", fileName);
            return List.of();
        }

        try (val in = Files.newBufferedReader(file.toPath())) {
            return mySerializers.stream()
                .filter(serializer -> serializer.supports(file))
                .map(serializer -> {
                    LOGGER.debug("Deserializing [{}] using [{}]", fileName, serializer.getClass().getSimpleName());
                    return serializer.load(in);
                })
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .peek(service -> {
                    if (service == null) {
                        LOGGER.debug("NULL service loaded from [{}]", fileName);
                    } else {
                        LOGGER.debug("Loaded service [{}] with ID [{}] and name [{}]",
                            service.getClass().getSimpleName(), service.getId(), service.getName());
                    }
                })
                .toList();
        } catch (IOException e) {
            LOGGER.error("Error loading service from [{}]: {}", fileName, e.getMessage(), e);
            return List.of();
        }
    }
    

    @Override
    public Collection<RegisteredService> load() {
        LOGGER.debug("CesDebugServiceRegistry.load() called");
    
        val dir = serviceRegistryDirectory.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            LOGGER.debug("Directory [{}] does not exist or is not a directory", dir.getAbsolutePath());
            return List.of();
        }
    
        val files = FileUtils.listFiles(dir, getExtensions(), true);
        LOGGER.debug("Found [{}] service file(s)", files.size());
    
        this.services = files.stream()
            .map(this::load) // <- custom file-level deserialization above
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .peek(service -> LOGGER.debug("Deserialized: name='{}', id='{}', class={}",
                service.getName(), service.getId(), service.getClass().getSimpleName()))
            .collect(Collectors.toMap(
                RegisteredService::getId,
                Function.identity(),
                (s1, s2) -> {
                    LOGGER.debug("Duplicate ID [{}], keeping first: [{}]", s2.getId(), s1.getName());
                    return s1;
                },
                LinkedHashMap::new
            ));
    
        return new ArrayList<>(this.services.values());
    }
    

    @Override
    protected String[] getExtensions() {
        return new String[] { "json" };
    }
}

