package de.triology.cas.pat.config.persistence;

import javax.sql.DataSource;

import de.triology.cas.pat.config.PATServiceProperties;

/**
 * Encapsulates database-vendor-specific PAT persistence setup.
 */
public interface PATDatabaseProvider {
    /**
     * Determines whether this provider owns the supplied JDBC URL.
     *
     * @param databaseUrl configured JDBC URL
     * @return whether this provider supports the URL
     */
    boolean supports(String databaseUrl);

    /**
     * Creates the vendor-specific data source.
     *
     * @param properties validated PAT database properties
     * @return PAT data source
     */
    DataSource createDataSource(PATServiceProperties properties);

    /**
     * Returns the vendor-specific Flyway migration location.
     *
     * @return classpath migration location
     */
    String migrationLocation();
}
