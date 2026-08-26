package de.triology.cas.pat.config;

import de.triology.cas.pat.config.persistence.PATDatabaseProvider;
import de.triology.cas.pat.config.persistence.SQLitePATDatabaseProvider;
import org.sqlite.SQLiteDataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Registers SQLite as the embedded PAT persistence provider.
 */
@AutoConfiguration(before = PATServiceConfiguration.class)
@ConditionalOnClass(SQLiteDataSource.class)
@ConditionalOnProperty(prefix = "custom-token-service", name = "enabled", havingValue = "true")
public class PATSQLitePersistenceConfiguration {

    /**
     * Creates the SQLite provider without exposing SQLite details to the common PAT configuration.
     *
     * @return SQLite PAT database provider
     */
    @Bean
    public PATDatabaseProvider sqlitePATDatabaseProvider() {
        return new SQLitePATDatabaseProvider();
    }
}
