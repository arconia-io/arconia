package io.arconia.dev.services.keycloak;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

/**
 * {@link ContainerConnectionDetailsFactory} that produces {@link KeycloakConnectionDetails}
 * from an {@link ArconiaKeycloakContainer}.
 */
class KeycloakContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<ArconiaKeycloakContainer, KeycloakConnectionDetails> {

    KeycloakContainerConnectionDetailsFactory() {}

    @Override
    protected KeycloakConnectionDetails getContainerConnectionDetails(
            ContainerConnectionSource<ArconiaKeycloakContainer> source) {
        return new KeycloakContainerConnectionDetails(source);
    }

    private static final class KeycloakContainerConnectionDetails
            extends ContainerConnectionDetails<ArconiaKeycloakContainer> implements KeycloakConnectionDetails {

        private KeycloakContainerConnectionDetails(ContainerConnectionSource<ArconiaKeycloakContainer> source) {
            super(source);
        }

        @Override
        public String getAuthServerUrl() {
            return getContainer().getAuthServerUrl();
        }

        @Override
        public String getRealm() {
            return getContainer().getRealm();
        }

        @Override
        public String getIssuerUri() {
            return getContainer().getIssuerUri();
        }

    }

}
