package io.arconia.dev.services.api.config;

import java.util.List;

import io.arconia.core.support.Incubating;

/**
 * Base properties for dev services that use JDBC.
 */
@Incubating
public interface JdbcDevServicesProperties extends BaseDevServicesProperties {

    String DEFAULT_USERNAME = "arconia";
    String DEFAULT_PASSWORD = "arconia";
    String DEFAULT_DB_NAME = "arconia";

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

    // SETTERS

    default void setUsername(String username) {}

    default void setPassword(String password) {}

    default void setDbName(String dbName) {}

    default void setInitScriptPaths(List<String> initScriptPaths) {}

}
