package io.arconia.dev.services.mysql;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.core.config.DevServicesProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Properties for the MySQL Dev Services.
 */
@ConfigurationProperties(prefix = MySqlDevServicesProperties.CONFIG_PREFIX)
public class MySqlDevServicesProperties implements DevServicesProperties {

    public static final String CONFIG_PREFIX = "arconia.dev.services.mysql";

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "mysql:9.3.0";

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
