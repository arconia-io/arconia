package io.arconia.dev.services.mysql;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.api.config.JdbcDevServicesProperties;

/**
 * Properties for the MySQL Dev Services.
 */
@ConfigurationProperties(prefix = MySqlDevServicesProperties.CONFIG_PREFIX)
public class MySqlDevServicesProperties implements JdbcDevServicesProperties {

    public static final String CONFIG_PREFIX = "arconia.dev.services.mysql";

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "mysql:8.4";

    /**
     * Environment variables to set in the service.
     */
    private Map<String,String> environment = new HashMap<>();

    /**
     * Network aliases to assign to the dev service container.
     */
    private List<String> networkAliases = new ArrayList<>();

    /**
     * Fixed port for exposing the MySQL database port to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int port = 0;

    /**
     * Whether the dev service is shared among applications.
     * Only applicable in dev mode.
     */
    private boolean shared = false;

    /**
     * Maximum waiting time for the service to start.
     */
    private Duration startupTimeout = Duration.ofSeconds(30);

    /**
     * Username to be used for connecting to the database.
     */
    private String username = "test";

    /**
     * Password to be used for connecting to the database.
     */
    private String password = "test";

    /**
     * Name of the database to be created.
     */
    private String dbName = "test";

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
