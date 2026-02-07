package io.arconia.dev.services.artemis;

import com.github.dockerjava.api.command.InspectContainerResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * An {@link ArtemisContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaArtemisContainer extends ArtemisContainer {

    private static final String COMPATIBLE_IMAGE_NAME = "apache/activemq-artemis";

    private static final Logger logger = LoggerFactory.getLogger(ArconiaArtemisContainer.class);

    private final ArtemisDevServicesProperties properties;

    // CORE, MQTT, AMQP, HORNETQ, STOMP, OPENWIRE
    static final int TCP_PORT = 61616;

    static final int WEB_CONSOLE_PORT = 8161;

    public ArconiaArtemisContainer(ArtemisDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        ContainerConfigurer.base(this, properties);

        this.withUser(StringUtils.hasText(properties.getUsername()) ? properties.getUsername() : ArtemisDevServicesProperties.DEFAULT_USERNAME);
        this.withPassword(StringUtils.hasText(properties.getPassword()) ? properties.getPassword() : ArtemisDevServicesProperties.DEFAULT_PASSWORD);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isValidPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), TCP_PORT);
        }
        if (ContainerUtils.isValidPort(properties.getManagementConsolePort())) {
            addFixedExposedPort(properties.getManagementConsolePort(), WEB_CONSOLE_PORT);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);
        logger.info("Artemis Management Console: {}", getManagementConsoleUrl());
    }

    /**
     * Retrieve the URL of the Artemis Management Console.
     */
    String getManagementConsoleUrl() {
        return "http://" + getHost() + ":" + getMappedPort(WEB_CONSOLE_PORT) + "/console";
    }

}
