package io.arconia.dev.services.kafka;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.core.config.DevServicesProperties;

/**
 * Properties for the Kafka Dev Services.
 */
@ConfigurationProperties(prefix = "arconia.dev.services.kafka")
public class KafkaDevServicesProperties implements DevServicesProperties {

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "apache/kafka-native:4.1.0";

    /**
     * Environment variables to set in the service.
     */
    private Map<String,String> environment = new HashMap<>();

    /**
     * When the dev service is shared across applications.
     */
    private Shared shared = Shared.DEV_MODE;

    /**
     * Maximum waiting time for the service to start.
     */
    private Duration startupTimeout = Duration.ofMinutes(2);

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

}
