package io.arconia.dev.services.keycloak;

import com.github.dockerjava.api.command.InspectContainerResponse;

import org.testcontainers.utility.DockerImageName;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import io.arconia.dev.services.core.container.ContainerConfigurer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;


/**
 * A {@link KeycloakContainer} specialized for Arconia Dev Services.
 */
public final class ArconiaKeycloakContainer extends KeycloakContainer {
    private static final Logger logger = LoggerFactory.getLogger(ArconiaKeycloakContainer.class);

    private final KeycloakDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "keycloak";

    static final int WEB_CONSOLE_PORT = 8080;

    public ArconiaKeycloakContainer(KeycloakDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME).asCanonicalNameString());
        this.properties = properties;

        ContainerConfigurer.base(this, properties);


        this.withAdminUsername(StringUtils.hasText(properties.getUsername()) ? properties.getUsername() : KeycloakDevServicesProperties.DEFAULT_USERNAME);
        this.withAdminPassword(StringUtils.hasText(properties.getPassword()) ? properties.getPassword() : KeycloakDevServicesProperties.DEFAULT_PASSWORD);
}

    @Override
    protected void configure() {
        super.configure();
        if (properties.getPort() > 0) {
            addFixedExposedPort(properties.getPort(), WEB_CONSOLE_PORT);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        logger.info("Keycloak Web Console: {}", getManagementConsoleUrl());
    }


    /**
     * Retrieve the URL of the Keycloak Web Console.
     */
    String getManagementConsoleUrl() {
        return "http://" + getHost() + ":" + getMappedPort(WEB_CONSOLE_PORT) + "/console";
    }
}
