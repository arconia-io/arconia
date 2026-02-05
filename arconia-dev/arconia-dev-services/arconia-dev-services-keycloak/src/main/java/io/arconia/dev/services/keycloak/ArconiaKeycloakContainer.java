package io.arconia.dev.services.keycloak;

import org.testcontainers.utility.DockerImageName;

import dasniko.testcontainers.keycloak.KeycloakContainer;

/**
 * A {@link KeycloakContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaKeycloakContainer extends KeycloakContainer {

    private final KeycloakDevServicesProperties properties;

    protected static final int WEB_CONSOLE_PORT = 8080;

    public ArconiaKeycloakContainer(DockerImageName dockerImageName, KeycloakDevServicesProperties properties) {
        super(dockerImageName.asCanonicalNameString());
        this.properties = properties;
    }

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), WEB_CONSOLE_PORT);
        }
    }
}
