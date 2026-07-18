package io.arconia.dev.services.core.autoconfigure;

import org.jspecify.annotations.Nullable;
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

    /**
     * Configuration for the shared network that dev service containers can join.
     */
    private final Network network = new Network();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Network getNetwork() {
        return network;
    }

    /**
     * Configuration for the shared network that dev service containers can join to
     * communicate with each other.
     */
    public static class Network {

        /**
         * Name of the OCI network dev service containers join. When set, containers
         * join a stable, named network that can be shared across applications running
         * simultaneously; the network is created if it doesn't already exist.
         * When unset (default), containers join an isolated per-application network.
         * Only applicable in dev mode.
         */
        private @Nullable String name;

        @Nullable
        public String getName() {
            return name;
        }

        public void setName(@Nullable String name) {
            this.name = name;
        }

    }

}
