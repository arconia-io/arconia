package io.arconia.dev.services.keycloak;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link KeycloakConnectionDetails} for connecting to a Keycloak dev service running
 * in a container discovered from another application.
 */
final class KeycloakDiscoveredConnectionDetails implements KeycloakConnectionDetails {

    private final String authServerUrl;

    private final String realm;

    KeycloakDiscoveredConnectionDetails(DiscoveredContainer container, KeycloakDevServicesProperties properties) {
        this.authServerUrl = "http://%s:%d".formatted(container.host(),
                container.mappedPort(ArconiaKeycloakContainer.HTTP_PORT));
        this.realm = KeycloakRealmMetadata.resolveRealmName(properties);
    }

    @Override
    public String getAuthServerUrl() {
        return authServerUrl;
    }

    @Override
    public String getRealm() {
        return realm;
    }

    @Override
    public String getIssuerUri() {
        return ArconiaKeycloakContainer.issuerUri(authServerUrl, realm);
    }

}
