package io.arconia.dev.services.core.config;

import java.util.List;

/**
 * Base properties for dev services that use JDBC.
 */
public interface JdbcDevServicesProperties extends DevServicesProperties {

    /**
     * Username to be used for connecting to the database.
     */
    String getUsername();

    /**
     * Password to be used for connecting to the database.
     */
    String getPassword();

    /**
     * Name of the database to be created.
     */
    String getDbName();

    /**
     * List of paths to SQL scripts to be loaded from the classpath and
     * applied to the database for initialization.
     */
    List<String> getInitScriptPaths();

}
