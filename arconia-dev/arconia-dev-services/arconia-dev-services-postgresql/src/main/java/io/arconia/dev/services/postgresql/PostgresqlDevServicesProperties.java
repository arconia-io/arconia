package io.arconia.dev.services.postgresql;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.core.config.JdbcDevServicesProperties;

/**
 * Properties for the PostgreSQL Dev Services.
 */
@ConfigurationProperties(prefix = "arconia.dev.services.postgresql")
public class PostgresqlDevServicesProperties implements JdbcDevServicesProperties {

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "postgres:18.1-alpine";

    /**
     * Environment variables to set in the service.
     */
    private Map<String,String> environment = new HashMap<>();

    /**
     * When the dev service is shared across applications.
     */
    private Shared shared = Shared.NEVER;

    /**
     * Maximum waiting time for the service to start.
     */
    private Duration startupTimeout = Duration.ofMinutes(2);

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
    public Shared getShared() {
        return shared;
    }

    public void setShared(Shared shared) {
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
