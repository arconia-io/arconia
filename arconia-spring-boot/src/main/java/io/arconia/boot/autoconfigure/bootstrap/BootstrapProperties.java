package io.arconia.boot.autoconfigure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for configuring the bootstrap profiles.
 */
@ConfigurationProperties(prefix = BootstrapProperties.CONFIG_PREFIX)
public class BootstrapProperties {

    public static final String CONFIG_PREFIX = "arconia.bootstrap";

    public final Profiles profiles = new Profiles();

    public static class Profiles {

        /**
         * Whether the profiles are enabled based on the application mode.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }

}
