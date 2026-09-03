package de.triology.cas.pat.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import de.triology.cas.pat.config.persistence.PATDatabaseProvider;
import de.triology.cas.pat.config.persistence.SQLitePATDatabaseProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

class SQLitePATDatabaseProviderTest {

    @TempDir
    java.nio.file.Path tempDir;

    @Test
    void supportsOnlySqliteUrlsAndDeclaresMigrationLocation() {
        SQLitePATDatabaseProvider provider = new SQLitePATDatabaseProvider();

        assertTrue(provider.supports("jdbc:sqlite:/tmp/pat.db"));
        assertFalse(provider.supports("jdbc:postgresql://db/pat"));
        assertFalse(provider.supports(null));
        assertEquals("classpath:db/pat/migration/sqlite", provider.migrationLocation());
    }

    @Test
    void createsConfiguredSqliteDataSource() throws Exception {
        PATServiceProperties properties = properties("jdbc:sqlite:" + tempDir.resolve("pat.db"));
        SQLitePATDatabaseProvider provider = new SQLitePATDatabaseProvider();

        SQLiteDataSource dataSource = (SQLiteDataSource) provider.createDataSource(properties);

        assertEquals(properties.getDatabaseUrl(), dataSource.getUrl());
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            assertEquals(5000, pragma(statement, "busy_timeout"));
            assertEquals(1, pragma(statement, "foreign_keys"));
            assertEquals("wal", pragmaText(statement, "journal_mode"));
        }
    }

    @Test
    void rejectsUnsupportedUrlWhenCreatingDataSource() {
        SQLitePATDatabaseProvider provider = new SQLitePATDatabaseProvider();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> provider.createDataSource(properties("jdbc:other:test")));

        assertEquals("SQLite PAT database URL must start with jdbc:sqlite:", exception.getMessage());
    }

    @Test
    void configurationCreatesProvider() {
        PATDatabaseProvider provider = new PATSQLitePersistenceConfiguration().sqlitePATDatabaseProvider();
        assertTrue(provider instanceof SQLitePATDatabaseProvider);
    }

    private static int pragma(Statement statement, String name) throws Exception {
        try (ResultSet result = statement.executeQuery("PRAGMA " + name)) {
            return result.getInt(1);
        }
    }

    private static String pragmaText(Statement statement, String name) throws Exception {
        try (ResultSet result = statement.executeQuery("PRAGMA " + name)) {
            return result.getString(1);
        }
    }

    private static PATServiceProperties properties(String url) {
        PATServiceProperties properties = new PATServiceProperties();
        properties.setEnabled(true);
        properties.setDatabaseUrl(url);
        return properties;
    }
}
