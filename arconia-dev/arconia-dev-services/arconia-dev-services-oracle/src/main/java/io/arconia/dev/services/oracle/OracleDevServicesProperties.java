package io.arconia.dev.services.oracle;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.api.config.JdbcDevServicesProperties;
import io.arconia.dev.services.api.config.ResourceMapping;

/**
 * Properties for the Oracle Dev Services.
 */
@ConfigurationProperties(prefix = OracleDevServicesProperties.CONFIG_PREFIX)
public class OracleDevServicesProperties implements JdbcDevServicesProperties {

    public static final String CONFIG_PREFIX = "arconia.dev.services.oracle";

    static final String DEFAULT_USERNAME = "arconia";
    static final String DEFAULT_PASSWORD = "arconia";
    static final String DEFAULT_DB_NAME = "arconia";

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "gvenzl/oracle-free:23.9-slim-faststart";

    /**
     * Environment variables to set in the service.
     */
    private Map<String,String> environment = new HashMap<>();

    /**
     * Network aliases to assign to the dev service container.
     */
    private List<String> networkAliases = new ArrayList<>();

    /**
     * Fixed port for exposing the Oracle database port to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int port = 0;

    /**
     * Resources from the classpath or host filesystem to copy into the container.
     * They can be files or directories that will be copied to the specified
     * destination path inside the container at startup and are immutable (read-only).
     */
    private List<ResourceMapping> resources = new ArrayList<>();

    /**
     * Whether the dev service is shared among applications.
     * Only applicable in dev mode.
     */
    private boolean shared = false;

    /**
     * Maximum waiting time for the service to start.
     */
    private Duration startupTimeout = Duration.ofMinutes(2);

    /**
     * Username to be used for connecting to the database.
     */
    private String username = DEFAULT_USERNAME;

    /**
     * Password to be used for connecting to the database.
     */
    private String password = DEFAULT_PASSWORD;

    /**
     * Name of the database to be created.
     */
    private String dbName = DEFAULT_DB_NAME;

    /**
     * List of paths to SQL scripts to be loaded from the classpath and
     * applied to the database for initialization.
     */
    private List<String> initScriptPaths = new ArrayList<>();

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    @Override
    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    @Override
    public List<String> getNetworkAliases() {
        return networkAliases;
    }

    public void setNetworkAliases(List<String> networkAliases) {
        this.networkAliases = networkAliases;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public List<ResourceMapping> getResources() {
        return resources;
    }

    public void setResources(List<ResourceMapping> resources) {
        this.resources = resources;
    }

    @Override
    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    @Override
    public Duration getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    @Override
    public List<String> getInitScriptPaths() {
        return initScriptPaths;
    }

    public void setInitScriptPaths(List<String> initScriptPaths) {
        this.initScriptPaths = initScriptPaths;
    }

}
