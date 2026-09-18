package io.arconia.dev.services.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for wiring an OAuth2 Resource Server to the Keycloak Dev Service.
 */
@ConfigurationProperties(prefix = KeycloakResourceServerDevServicesProperties.CONFIG_PREFIX)
public class KeycloakResourceServerDevServicesProperties {

    public static final String CONFIG_PREFIX = KeycloakDevServicesProperties.CONFIG_PREFIX + ".resource-server";

    /**
     * Whether the resource server issuer is configured automatically from the dev service.
     * Disable it to keep the dev service running while configuring the issuer yourself.
     */
    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
