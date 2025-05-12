package io.arconia.dev.services.oracle;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.core.config.DevServicesProperties;

/**
 * Properties for the OracleXe Dev Services.
 */
@ConfigurationProperties(prefix = OracleXeDevServicesProperties.CONFIG_PREFIX)
public class OracleXeDevServicesProperties implements DevServicesProperties {

    public static final String CONFIG_PREFIX = "arconia.dev.services.oracle-xe";

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "gvenzl/oracle-xe:21-slim-faststart";

    /**
     * The maximum waiting time for the container to start.
     */
    private Duration startupTimeout = Duration.ofSeconds(60);

    /**
     * Environment variables to set in the container.
     */
    private Map<String,String> environment = new HashMap<>();

    /**
     * Whether the container used in the dev service is reusable across applications.
     */
    private boolean reusable = false;

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

    public Duration getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
    }

    @Override
    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    @Override
    public boolean isReusable() {
        return reusable;
    }

    public void setReusable(boolean reusable) {
        this.reusable = reusable;
    }

}
