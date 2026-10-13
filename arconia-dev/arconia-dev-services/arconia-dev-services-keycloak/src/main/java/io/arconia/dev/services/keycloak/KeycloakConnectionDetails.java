package io.arconia.dev.services.keycloak;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

import io.arconia.core.support.Incubating;

/**
 * Details for connecting to a Keycloak dev service.
 * <p>
 * These describe how to reach the service, not how an application authenticates against it:
 * the client identifier and secret are application configuration and live in
 * {@link KeycloakDevServicesProperties.Client} instead.
 */
@Incubating
public interface KeycloakConnectionDetails extends ConnectionDetails {

    /**
     * The URL of the Keycloak server.
     */
    String getAuthServerUrl();

    /**
     * The realm name.
     */
    String getRealm();

    /**
     * The issuer URI of the realm, as it appears in the tokens Keycloak issues.
     */
    default String getIssuerUri() {
        return "%s/realms/%s".formatted(getAuthServerUrl(), getRealm());
    }

    /**
     * The URI of the JSON Web Key Set the realm signs its tokens with.
     */
    default String getJwkSetUri() {
        return getIssuerUri() + "/protocol/openid-connect/certs";
    }

}
