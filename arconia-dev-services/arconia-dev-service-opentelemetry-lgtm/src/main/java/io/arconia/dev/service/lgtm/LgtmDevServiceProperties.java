package io.arconia.dev.service.lgtm;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.service.core.config.DevServiceProperties;

/**
 * Properties for the OpenTelemetry LGTM Dev Service.
 */
@ConfigurationProperties(prefix = LgtmDevServiceProperties.CONFIG_PREFIX)
public class LgtmDevServiceProperties implements DevServiceProperties {

    public static final String CONFIG_PREFIX = "arconia.dev.services.lgtm";

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "grafana/otel-lgtm:0.8.6";

    /**
     * Whether the container used in the dev service is reusable across applications.
     */
    private boolean reusable = true;

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
