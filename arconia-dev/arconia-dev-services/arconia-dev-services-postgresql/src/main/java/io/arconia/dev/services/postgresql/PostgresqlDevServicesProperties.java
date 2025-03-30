package io.arconia.dev.services.postgresql;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.core.config.DevServicesProperties;

/**
 * Properties for the PostgreSQL Dev Services.
 */
@ConfigurationProperties(prefix = PostgresqlDevServicesProperties.CONFIG_PREFIX)
public class PostgresqlDevServicesProperties implements DevServicesProperties {

    public static final String CONFIG_PREFIX = "arconia.dev.services.postgresql";

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "postgres:17.4-alpine";

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
    public boolean isReusable() {
        return reusable;
    }

    public void setReusable(boolean reusable) {
        this.reusable = reusable;
    }

}
