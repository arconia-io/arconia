package io.arconia.dev.services.core.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Global configuration properties for Dev Services.
 */
@ConfigurationProperties(prefix = DevServicesProperties.CONFIG_PREFIX)
public class DevServicesProperties {

    public static final String CONFIG_PREFIX = "arconia.dev.services";

    /**
     * Whether to enable the Dev Services feature.
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
