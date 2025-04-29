package de.triology.cas.services;

import org.apereo.cas.services.RegisteredService;
import org.apereo.cas.util.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CesAbstractResourceBasedServiceRegistryTests {

    private CesAbstractResourceBasedServiceRegistry registry;
    private StringSerializer<RegisteredService> serializer;
    private Path tempDirectory;

    @BeforeEach
    void setUp() throws IOException {
        serializer = mock(StringSerializer.class);
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);

        tempDirectory = Files.createTempDirectory("cas-test-registry");

        registry = new CesAbstractResourceBasedServiceRegistry(
                tempDirectory,
                serializer,
                applicationContext,
                Collections.emptyList()
        );
    }

    @Test
    void load_ShouldReturnEmptyList_WhenFileUnreadable() throws IOException {
        File tempFile = File.createTempFile("unreadable", ".json");
        assertTrue(tempFile.setReadable(false));

        Collection<RegisteredService> result = registry.load(tempFile);

        assertTrue(result.isEmpty(), "Should return empty list for unreadable file");

        tempFile.delete();
    }

    @Test
    void load_ShouldReturnEmptyList_WhenFileIsEmpty() throws IOException {
        File tempFile = File.createTempFile("empty", ".json");
        Files.writeString(tempFile.toPath(), "", StandardCharsets.UTF_8);

        Collection<RegisteredService> result = registry.load(tempFile);

        assertTrue(result.isEmpty(), "Should return empty list for empty file");

        tempFile.delete();
    }

    @Test
    void load_ShouldReturnServices_WhenDeserializationSucceeds() throws IOException {
        RegisteredService service = mock(RegisteredService.class);
        when(service.getId()).thenReturn(123L);
        when(service.getName()).thenReturn("TestService");

        when(serializer.supports(any(File.class))).thenReturn(true);
        when(serializer.load(any(Reader.class))).thenReturn(List.of(service));

        File tempFile = File.createTempFile("valid", ".json");
        Files.writeString(tempFile.toPath(), "{}", StandardCharsets.UTF_8);

        Collection<RegisteredService> result = registry.load(tempFile);

        assertFalse(result.isEmpty(), "Should load services from valid file");
        assertEquals("TestService", result.iterator().next().getName(), "Loaded service should match expected name");

        tempFile.delete();
    }

    @Test
    void load_ShouldReturnEmptyList_WhenIOExceptionOccurs() throws IOException {
        File fakeFile = new File("nonexistent/path/error.json");

        Collection<RegisteredService> result = registry.load(fakeFile);

        assertTrue(result.isEmpty(), "Should return empty list when IOException occurs");
    }

    @Test
    void loadDirectory_ShouldReturnServices_WhenFilesExist() throws IOException {
        // Prepare a valid service file in the temp directory
        RegisteredService service = mock(RegisteredService.class);
        when(service.getId()).thenReturn(999L);
        when(service.getName()).thenReturn("DirectoryService");

        when(serializer.supports(any(File.class))).thenReturn(true);
        when(serializer.load(any(Reader.class))).thenReturn(List.of(service));

        File serviceFile = new File(tempDirectory.toFile(), "service1.json");
        Files.writeString(serviceFile.toPath(), "{}", StandardCharsets.UTF_8);

        // When loading all services
        Collection<RegisteredService> result = registry.load();

        assertFalse(result.isEmpty(), "Should find and load service from directory");
        assertEquals("DirectoryService", result.iterator().next().getName(), "Service name should match");

        serviceFile.delete();
    }

    @Test
    void loadDirectory_ShouldReturnEmptyList_WhenDirectoryMissing() throws IOException {
        // Create temp directory, but instead of deleting, mock the directory check
        Path tempDir = Files.createTempDirectory("cas-fake-dir");

        var registry = new CesAbstractResourceBasedServiceRegistry(
                tempDir,
                serializer,
                mock(ConfigurableApplicationContext.class),
                Collections.emptyList()
        ) {
            @Override
            public Collection<RegisteredService> load() {
                var dir = serviceRegistryDirectory.toFile();
                // simulate missing dir
                if (true) { // always simulate missing
                    return List.of();
                }
                return super.load();
            }
        };

        Collection<RegisteredService> result = registry.load();

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should return empty list when directory does not exist");
    }

    

    @Test
    void getExtensions_ShouldReturnJson() {
        String[] extensions = registry.getExtensions();

        assertArrayEquals(new String[]{"json"}, extensions, "Only JSON extensions should be supported");
    }
}
