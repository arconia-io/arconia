package io.arconia.dev.services.keycloak;

import io.arconia.dev.services.core.registration.DiscoveredContainer;

/**
 * {@link KeycloakConnectionDetails} for connecting to a shared Keycloak dev service running
 * in a container discovered from another application.
 * <p>
 * The realm comes from the local configuration properties: it must match the realm imported
 * by the application that started the shared container. An application joining a shared dev
 * service therefore does not need the realm file itself, only the realm name — but note that
 * ownership goes to whichever application starts first, so configuring the same realm import
 * path everywhere is what makes the setup independent of startup order.
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
        return "%s/realms/%s".formatted(authServerUrl, realm);
    }

}
