package io.arconia.dev.services.keycloak;

import io.arconia.core.support.Incubating;
import io.arconia.dev.services.oidc.OidcConnectionDetails;

/**
 * Details for connecting to a Keycloak dev service.
 * <p>
 * These describe how to reach the service, not how an application authenticates against it:
 * the client identifier and secret are application configuration and live in
 * {@link KeycloakDevServicesProperties.Client} instead.
 */
@Incubating
public interface KeycloakConnectionDetails extends OidcConnectionDetails {

    /**
     * The URL of the Keycloak server.
     */
    String getAuthServerUrl();

    /**
     * The realm name.
     */
    String getRealm();

    @Override
    default String getIssuerUri() {
        return "%s/realms/%s".formatted(getAuthServerUrl(), getRealm());
    }

    @Override
    default String getJwkSetUri() {
        return getIssuerUri() + "/protocol/openid-connect/certs";
    }

}
