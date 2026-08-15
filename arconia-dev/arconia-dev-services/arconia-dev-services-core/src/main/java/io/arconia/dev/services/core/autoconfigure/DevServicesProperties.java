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
         * Whether dev service containers join a shared network so they can reach each other
         * by network alias (for example, to send telemetry to an observability dev service).
         * When disabled (default), each container uses its own isolated network. When enabled,
         * all dev service containers join the same network. Only applicable in dev mode.
         */
        private boolean enabled = false;

        /**
         * Name of the OCI network dev service containers join. When set, containers
         * join a stable, named network that can be shared across applications running
         * simultaneously; the network is created if it doesn't already exist. A named
         * network is required for container reuse to remain effective while networking
         * is enabled. When unset (default), containers join an isolated per-application
         * network. Only applicable in dev mode.
         */
        private @Nullable String name;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Nullable
        public String getName() {
            return name;
        }

        public void setName(@Nullable String name) {
            this.name = name;
        }

    }

}
