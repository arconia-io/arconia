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

    /**
     * Configuration for the network shared by the dev service containers of the application.
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
     * Configuration for the network shared by the dev service containers of the application.
     */
    public static class Network {

        /**
         * Whether dev service containers join a network shared by all the dev service containers
         * of the application, so they can reach each other by service name.
         * When disabled (default), each container uses the default network of the container runtime.
         */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }

}
