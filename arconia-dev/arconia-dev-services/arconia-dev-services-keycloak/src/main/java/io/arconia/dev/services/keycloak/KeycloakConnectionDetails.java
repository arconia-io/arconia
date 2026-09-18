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

    String getAuthServerUrl();

    String getRealm();

    String getIssuerUri();

}
