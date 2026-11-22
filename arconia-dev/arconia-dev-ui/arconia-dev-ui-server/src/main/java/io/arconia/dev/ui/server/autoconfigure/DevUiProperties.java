package io.arconia.dev.ui.server.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Dev UI.
 */
@ConfigurationProperties(prefix = DevUiProperties.CONFIG_PREFIX)
class DevUiProperties {

    static final String CONFIG_PREFIX = "arconia.dev.ui";

    /**
     * Whether to enable the Dev UI.
     */
    private boolean enabled = true;

    /**
     * Host names allowed to access the Dev UI, in addition to the local machine
     * ("localhost", "127.0.0.1", "::1"). For example, the host name of a remote
     * development environment.
     */
    private List<String> allowedHosts = new ArrayList<>();

    /**
     * Configuration for the welcome page and the error pages of the Dev UI.
     */
    private final ErrorPages errorPages = new ErrorPages();

    /**
     * Configuration for the observations of the requests to the Dev UI.
     */
    private final Observations observations = new Observations();

    /**
     * Configuration for the access to the Dev UI from other machines.
     */
    private final RemoteAccess remoteAccess = new RemoteAccess();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getAllowedHosts() {
        return allowedHosts;
    }

    public void setAllowedHosts(List<String> allowedHosts) {
        this.allowedHosts = allowedHosts;
    }

    public ErrorPages getErrorPages() {
        return errorPages;
    }

    public Observations getObservations() {
        return observations;
    }

    public RemoteAccess getRemoteAccess() {
        return remoteAccess;
    }

    /**
     * Configuration for the welcome page and the error pages of the Dev UI.
     */
    public static class ErrorPages {

        /**
         * Whether to show the welcome page and the error pages of the Dev UI in place of the
         * whitelabel error page.
         */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }

    /**
     * Configuration for the observations of the requests to the Dev UI.
     */
    public static class Observations {

        /**
         * Whether to include the requests to the Dev UI in the metrics and traces of the
         * application.
         */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }

    /**
     * Configuration for the access to the Dev UI from other machines.
     */
    public static class RemoteAccess {

        /**
         * Whether to accept the requests addressed to the local machine ("localhost",
         * "127.0.0.1", "::1") that come from another machine. For example, when the
         * application runs in a container and is reached through a published port.
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
