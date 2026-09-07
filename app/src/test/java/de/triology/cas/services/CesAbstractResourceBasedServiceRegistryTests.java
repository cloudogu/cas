package de.triology.cas.services;

import org.apache.commons.io.FileUtils;
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

    // --- helpers -----------------------------------------------------------

    private static File tempFile(String prefix, String content) throws IOException {
        File file = File.createTempFile(prefix, ".json");
        file.deleteOnExit();
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return file;
    }

    private File fileInRegistryDir(String name, String content) throws IOException {
        File file = new File(tempDirectory.toFile(), name);
        file.deleteOnExit();
        Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return file;
    }

    private static RegisteredService serviceStub(long id, String name) {
        RegisteredService service = mock(RegisteredService.class);
        when(service.getId()).thenReturn(id);
        when(service.getName()).thenReturn(name);
        return service;
    }

    // --- load(File) ----------------------------------------------------------

    @Test
    void load_ShouldReturnEmptyList_WhenFileUnreadable() throws IOException {
        File tempFile = tempFile("unreadable", "");
        assertTrue(tempFile.setReadable(false));

        Collection<RegisteredService> result = registry.load(tempFile);

        assertTrue(result.isEmpty(), "Should return empty list for unreadable file");
    }

    @Test
    void load_ShouldReturnEmptyList_WhenFileIsEmpty() throws IOException {
        File tempFile = tempFile("empty", "");

        Collection<RegisteredService> result = registry.load(tempFile);

        assertTrue(result.isEmpty(), "Should return empty list for empty file");
    }

    @Test
    void load_ShouldReturnServices_WhenDeserializationSucceeds() throws IOException {
        RegisteredService service = serviceStub(123L, "TestService");
        when(serializer.supports(any(File.class))).thenReturn(true);
        when(serializer.load(any(Reader.class))).thenReturn(List.of(service));
        File tempFile = tempFile("valid", "{}");

        Collection<RegisteredService> result = registry.load(tempFile);

        assertFalse(result.isEmpty(), "Should load services from valid file");
        assertEquals("TestService", result.iterator().next().getName(), "Loaded service should match expected name");
    }

    @Test
    void load_ShouldReturnEmptyList_WhenIOExceptionOccurs() {
        File fakeFile = new File("nonexistent/path/error.json");

        Collection<RegisteredService> result = registry.load(fakeFile);

        assertTrue(result.isEmpty(), "Should return empty list when IOException occurs");
    }

    @Test
    void load_ShouldReturnEmptyList_WhenFileNameStartsWithDot() throws IOException {
        File hiddenFile = fileInRegistryDir(".hidden.json", "{}");

        Collection<RegisteredService> result = registry.load(hiddenFile);

        assertTrue(result.isEmpty(), "Should skip hidden (dot-prefixed) files");
    }

    @Test
    void load_ShouldReturnEmptyList_WhenSerializerSupportsThrows() throws IOException {
        when(serializer.supports(any(File.class))).thenThrow(new RuntimeException("boom"));
        File tempFile = tempFile("supports-throws", "{}");

        Collection<RegisteredService> result = registry.load(tempFile);

        assertTrue(result.isEmpty(), "Should return empty list when serializer.supports() throws");
    }

    @Test
    void load_ShouldReturnEmptyList_WhenSerializerLoadReturnsNull() throws IOException {
        when(serializer.supports(any(File.class))).thenReturn(true);
        when(serializer.load(any(Reader.class))).thenReturn(null);
        File tempFile = tempFile("load-null", "{}");

        Collection<RegisteredService> result = registry.load(tempFile);

        assertTrue(result.isEmpty(), "Should return empty list when serializer.load() returns null");
    }

    @Test
    void load_ShouldReturnEmptyList_WhenSerializerLoadThrows() throws IOException {
        when(serializer.supports(any(File.class))).thenReturn(true);
        when(serializer.load(any(Reader.class))).thenThrow(new RuntimeException("boom"));
        File tempFile = tempFile("load-throws", "{}");

        Collection<RegisteredService> result = registry.load(tempFile);

        assertTrue(result.isEmpty(), "Should return empty list when serializer.load() throws");
    }

    // --- load() (directory scan) ---------------------------------------------

    @Test
    void loadDirectory_ShouldReturnServices_WhenFilesExist() throws IOException {
        RegisteredService service = serviceStub(999L, "DirectoryService");
        when(serializer.supports(any(File.class))).thenReturn(true);
        when(serializer.load(any(Reader.class))).thenReturn(List.of(service));
        fileInRegistryDir("service1.json", "{}");

        Collection<RegisteredService> result = registry.load();

        assertFalse(result.isEmpty(), "Should find and load service from directory");
        assertEquals("DirectoryService", result.iterator().next().getName(), "Service name should match");
    }

    @Test
    void loadDirectory_ShouldReturnEmptyList_WhenDirectoryMissing() throws IOException {
        // Directory existed at construction time (passing the fail-fast check) but is removed
        // before load() is called, e.g. deleted concurrently on disk.
        FileUtils.deleteDirectory(tempDirectory.toFile());

        Collection<RegisteredService> result = registry.load();

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should return empty list when directory does not exist");
    }

    @Test
    void loadDirectory_ShouldReturnEmptyList_WhenNoFilesFound() {
        // tempDirectory exists but is empty (no .json files written into it)
        Collection<RegisteredService> result = registry.load();

        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should return empty list when the directory has no matching files");
    }

    @Test
    void loadDirectory_ShouldKeepFirst_WhenDuplicateIdsFound() throws IOException {
        RegisteredService first = serviceStub(42L, "First");
        RegisteredService duplicate = serviceStub(42L, "Duplicate");
        when(serializer.supports(any(File.class))).thenReturn(true);
        when(serializer.load(any(Reader.class))).thenReturn(List.of(first, duplicate));
        fileInRegistryDir("duplicates.json", "{}");

        Collection<RegisteredService> result = registry.load();

        assertEquals(1, result.size(), "Duplicate IDs should be de-duplicated");
        assertEquals("First", result.iterator().next().getName(), "The first service with a given ID should win");
    }

    @Test
    void loadDirectory_ShouldSkipNullService_WhenSerializerReturnsNullElement() throws IOException {
        RegisteredService real = serviceStub(7L, "Real");
        List<RegisteredService> withNull = new ArrayList<>();
        withNull.add(null);
        withNull.add(real);
        when(serializer.supports(any(File.class))).thenReturn(true);
        when(serializer.load(any(Reader.class))).thenReturn(withNull);
        fileInRegistryDir("with-null.json", "{}");

        Collection<RegisteredService> result = registry.load();

        assertEquals(1, result.size(), "Null services in the loaded collection should be skipped");
        assertEquals("Real", result.iterator().next().getName());
    }

    // --- construction & misc --------------------------------------------------

    @Test
    void constructor_ShouldThrow_WhenDirectoryReallyDoesNotExist() throws IOException {
        // CAS's own AbstractResourceBasedServiceRegistry validates the directory eagerly at
        // construction time (fail-fast) - it never reaches load() for a genuinely missing directory.
        Path missingDir = Files.createTempDirectory("cas-missing-dir");
        Files.delete(missingDir);

        assertThrows(IllegalArgumentException.class, () -> new CesAbstractResourceBasedServiceRegistry(
                missingDir,
                serializer,
                mock(ConfigurableApplicationContext.class),
                Collections.emptyList()
        ));
    }

    @Test
    void getExtensions_ShouldReturnJson() {
        String[] extensions = registry.getExtensions();

        assertArrayEquals(new String[]{"json"}, extensions, "Only JSON extensions should be supported");
    }
}
