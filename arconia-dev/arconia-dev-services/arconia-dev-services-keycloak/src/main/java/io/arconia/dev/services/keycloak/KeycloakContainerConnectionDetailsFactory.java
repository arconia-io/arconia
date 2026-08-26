package io.arconia.dev.services.keycloak;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

import dasniko.testcontainers.keycloak.KeycloakContainer;

/**
 * Factory for creating {@link KeycloakConnectionDetails} for Keycloak containers.
 */
public class KeycloakContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<KeycloakContainer, KeycloakConnectionDetails> {

    // private static final String CONNECTION_NAME = "keycloak";

    // public KeycloakContainerConnectionDetailsFactory() {
    //     super(CONNECTION_NAME);
    // }

    @Override
    protected KeycloakConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<KeycloakContainer> source) {
        return new KeycloakContainerConnectionDetails(source);
    }

    private static final class KeycloakContainerConnectionDetails extends ContainerConnectionDetails<KeycloakContainer>
            implements KeycloakConnectionDetails {

        private KeycloakContainerConnectionDetails(ContainerConnectionSource<KeycloakContainer> source) {
            super(source);
        }

        @Override
        public String getServerUrl() {
            return getContainer().getAuthServerUrl();
        }

        @Override
        public String getIssuerUri() {
            String auth = getContainer().getAuthServerUrl();
            return auth.endsWith("/") ? auth + "realms/master" : auth + "/realms/master";
        }
    }

}
