package io.arconia.dev.services.keycloak;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Connection details for Keycloak containers.
 */
public interface KeycloakConnectionDetails extends ConnectionDetails {

    String getServerUrl();

    String getIssuerUri();

}
