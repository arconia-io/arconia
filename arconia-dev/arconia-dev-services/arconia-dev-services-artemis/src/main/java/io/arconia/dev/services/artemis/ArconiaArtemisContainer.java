package io.arconia.dev.services.artemis;

import java.util.List;

import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * An {@link ArtemisContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaArtemisContainer extends ArtemisContainer implements DevServiceLinkProvider {

    private final ArtemisDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "apache/activemq-artemis";

    // CORE, MQTT, AMQP, HORNETQ, STOMP, OPENWIRE
    static final int TCP_PORT = 61616;

    static final int WEB_CONSOLE_PORT = 8161;

    public ArconiaArtemisContainer(ArtemisDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        ContainerConfigurer.base(this, properties);

        this.withUser(properties.getUsername());
        this.withPassword(properties.getPassword());
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isFixedPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), TCP_PORT);
        }
        if (ContainerUtils.isFixedPort(properties.getManagementConsolePort())) {
            addFixedExposedPort(properties.getManagementConsolePort(), WEB_CONSOLE_PORT);
        }
    }

    @Override
    public List<DevServiceLink> devServiceLinks() {
        return List.of(DevServiceLink.builder()
                .id("artemis")
                .label("Artemis Management Console")
                .url(getManagementConsoleUrl())
                .build());
    }

    /**
     * Retrieve the URL of the Artemis Management Console.
     */
    String getManagementConsoleUrl() {
        return "http://" + getHost() + ":" + getMappedPort(WEB_CONSOLE_PORT) + "/console";
    }

}
