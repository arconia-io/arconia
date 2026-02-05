package io.arconia.dev.services.keycloak;

import org.springframework.util.StringUtils;
import org.testcontainers.utility.DockerImageName;

import dasniko.testcontainers.keycloak.KeycloakContainer;

/**
 * Factory to create ArconiaKeycloakContainer instances from properties.
 */
public final class KeycloakContainerFactory {

    private static final String COMPATIBLE_IMAGE_NAME = "keycloak";

    private KeycloakContainerFactory() {
    }

    public static KeycloakContainer create(KeycloakDevServicesProperties properties) {

    return  new ArconiaKeycloakContainer(DockerImageName.parse(properties.getImageName())
                .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME), properties)
                .withEnv(properties.getEnvironment())
                .withStartupTimeout(properties.getStartupTimeout())
                .withReuse(properties.getShared().asBoolean())
                .withAdminUsername(StringUtils.hasText(properties.getUsername()) ? properties.getUsername() : KeycloakDevServicesProperties.DEFAULT_USERNAME)
                .withAdminPassword(StringUtils.hasText(properties.getPassword()) ? properties.getPassword() : KeycloakDevServicesProperties.DEFAULT_PASSWORD);

    }

}
