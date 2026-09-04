package de.triology.cas.pat.config.persistence;

import javax.sql.DataSource;

import de.triology.cas.pat.config.PATServiceProperties;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

/**
 * Creates and configures the embedded SQLite PAT database.
 */
public final class SQLitePATDatabaseProvider implements PATDatabaseProvider {
    private static final String JDBC_PREFIX = "jdbc:sqlite:";
    private static final int BUSY_TIMEOUT_MILLIS = 5_000;

    /**
     * Checks the condition handled by supports.
     */
    @Override
    public boolean supports(String databaseUrl) {
        return databaseUrl != null && databaseUrl.startsWith(JDBC_PREFIX);
    }

    /**
     * Creates a value using createDataSource.
     */
    @Override
    public DataSource createDataSource(PATServiceProperties properties) {
        String databaseUrl = properties.getDatabaseUrl();
        if (!supports(databaseUrl)) {
            throw new IllegalArgumentException("SQLite PAT database URL must start with " + JDBC_PREFIX);
        }

        SQLiteConfig sqliteConfig = new SQLiteConfig();
        sqliteConfig.setBusyTimeout(BUSY_TIMEOUT_MILLIS);
        sqliteConfig.enforceForeignKeys(true);
        sqliteConfig.setJournalMode(SQLiteConfig.JournalMode.WAL);

        SQLiteDataSource dataSource = new SQLiteDataSource(sqliteConfig);
        dataSource.setUrl(databaseUrl);
        return dataSource;
    }

    /**
     * Executes the migrationLocation operation.
     */
    @Override
    public String migrationLocation() {
        return "classpath:db/pat/migration/sqlite";
    }
}
