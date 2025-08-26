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
import java.nio.file.attribute.FileTime;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;


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
        final String fileName = file.getName();
        LOGGER.debug("Trying to load: {}", fileName);

        if (!file.exists() || !file.canRead() || file.length() == 0 || fileName.startsWith(".")) {
            LOGGER.debug("Skipping unreadable/empty/hidden file: {} (exists={}, canRead={}, length={})",
                    fileName, file.exists(), file.canRead(), file.length());
            return List.of();
        }

        // --- File metadata + raw head dump (safe size) ---
        try {
            long size = Files.size(file.toPath());
            FileTime lm = Files.getLastModifiedTime(file.toPath());
            LOGGER.debug("File [{}]: size={} bytes, lastModified={}", fileName, size, lm);

            // Dump first N characters to help see what's actually in there (avoid huge spam)
            final int MAX_CHARS = 2000;      // tune as needed
            String raw = Files.readString(file.toPath());
            String head = raw.length() > MAX_CHARS ? raw.substring(0, MAX_CHARS) + "\n...[truncated]..." : raw;
            LOGGER.debug("RAW HEAD [{}]:\n{}", fileName, head);
        } catch (Exception metaEx) {
            LOGGER.debug("Could not read metadata/raw head for [{}]: {}", fileName, metaEx.getMessage());
        }

        // --- Stream over serializers ---
        final AtomicInteger checked = new AtomicInteger();
        final AtomicInteger supported = new AtomicInteger();

        List<RegisteredService> result = mySerializers.stream()
            // log every serializer we check (INFO so it shows up)
            .peek(s -> {
                int n = checked.incrementAndGet();
                LOGGER.debug("#{} Checking serializer [{}] for file [{}]",
                        n, s.getClass().getSimpleName(), fileName);
            })

            // filter by supports(...) and count
            .filter(serializer -> {
                boolean ok = false;
                try {
                    ok = serializer.supports(file);
                } catch (Exception ex) {
                    LOGGER.error("serializer.supports() threw for [{}]: {}", serializer.getClass().getSimpleName(), ex.getMessage(), ex);
                }
                if (ok) {
                    int n = supported.incrementAndGet();
                    LOGGER.debug("#{} Serializer [{}] SUPPORTS [{}]",
                            n, serializer.getClass().getSimpleName(), fileName);
                } else {
                    LOGGER.debug("Serializer [{}] does NOT support [{}]",
                            serializer.getClass().getSimpleName(), fileName);
                }
                return ok;
            })

            // attempt to load; give each serializer its own fresh reader
            .map(serializer -> {
                try (var in = Files.newBufferedReader(file.toPath())) {
                    LOGGER.debug("Deserializing [{}] using [{}]",
                            fileName, serializer.getClass().getSimpleName());
                    Collection<RegisteredService> services = serializer.load(in);
                    if (services == null || services.isEmpty()) {
                        LOGGER.debug("Serializer [{}] returned NO services for [{}]",
                                serializer.getClass().getSimpleName(), fileName);
                    } else {
                        LOGGER.debug("Serializer [{}] returned {} service(s) for [{}]",
                                serializer.getClass().getSimpleName(), services.size(), fileName);
                    }
                    return services;
                } catch (Exception ex) {
                    LOGGER.error("Serializer [{}] failed for [{}]: {}",
                            serializer.getClass().getSimpleName(), fileName, ex.getMessage(), ex);
                    return null;
                }
            })

            .filter(Objects::nonNull)
            .flatMap(Collection::stream)

            // log each loaded service at WARN so it’s visible
            .peek(svc -> LOGGER.debug("Loaded service [{}] id=[{}] name=[{}]",
                    svc.getClass().getSimpleName(), svc.getId(), svc.getName()))

            .collect(Collectors.toList());

        LOGGER.debug("Summary for [{}]: serializers checked={}, supported={}, services loaded={}",
                fileName, checked.get(), supported.get(), result.size());

        if (result.isEmpty()) {
            LOGGER.debug("No services loaded from [{}]. Either no serializer supported it or all failed.", fileName);
        }

        return result;
    }


    @Override
    public Collection<RegisteredService> load() {
        LOGGER.debug("CesDebugServiceRegistry.load() called");

        final File dir = serviceRegistryDirectory.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            LOGGER.debug("[load()] Directory [{}] does not exist or is not a directory", dir.getAbsolutePath());
            return Collections.emptyList();
        }

        final Collection<File> files;
        try {
            files = FileUtils.listFiles(dir, getExtensions(), true);
        } catch (Exception ex) {
            LOGGER.error("[load()] Failed to list files in [{}]: {}", dir.getAbsolutePath(), ex.getMessage(), ex);
            return Collections.emptyList();
        }

        LOGGER.debug("[load()] Scanning [{}], found [{}] service file(s)", dir.getAbsolutePath(), files.size());
        if (files.isEmpty()) {
            return Collections.emptyList();
        }

        files.forEach(f -> 
            LOGGER.debug("[load] Candidate service file: [{}] (size={} bytes, lastModified={})",
                f.getAbsolutePath(), f.length(), f.lastModified())
        );

        // Build a fresh map locally to avoid leaving this.services null on failure.
        final Map<Long, RegisteredService> map = new LinkedHashMap<>();

        for (File file : files) {
            LOGGER.debug("[load()] Attempting to load file [{}]", file.getAbsolutePath());
            Collection<RegisteredService> col = Collections.emptyList();
            try {
                Collection<RegisteredService> tmp = this.load(file); // your file-level loader
                if (tmp == null) {
                    LOGGER.debug("[load()] File [{}] produced NULL services (treating as empty)", file.getAbsolutePath());
                } else if (tmp.isEmpty()) {
                    LOGGER.debug("[load()] File [{}] produced NO services", file.getAbsolutePath());
                } else {
                    LOGGER.debug("[load()] File [{}] produced [{}] service(s)", file.getAbsolutePath(), tmp.size());
                    col = tmp;
                }
            } catch (Exception ex) {
                LOGGER.error("[load()] Error loading [{}]: {}", file.getAbsolutePath(), ex.getMessage(), ex);
            }

            for (RegisteredService svc : col) {
                if (svc == null) {
                    LOGGER.debug("[load()] Skipping NULL service from file [{}]", file.getAbsolutePath());
                    continue;
                }
                LOGGER.debug("[load()] Deserialized: name='{}', id='{}', class={}",
                    svc.getName(), svc.getId(), svc.getClass().getSimpleName());

                final long id = svc.getId();
                if (map.containsKey(id)) {
                    LOGGER.debug("[load()] Duplicate ID [{}], keeping first: old='{}', new='{}'",
                        id, map.get(id).getName(), svc.getName());
                    continue;
                }
                map.put(id, svc);
            }
        }

        // Atomically replace internal state; never leave it null.
        this.services = map;

        LOGGER.debug("Finished registry load: [{}] total unique service(s) loaded", map.size());
        // Always return a non-null collection
        return new ArrayList<>(map.values());
    }

    

    @Override
    protected String[] getExtensions() {
        return new String[] { "json" };
    }
}

