package io.arconia.dev.services.pulsar;

import java.util.List;

import org.testcontainers.pulsar.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.api.registration.DevServiceLinkDefinition;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;

/**
 * A {@link PulsarContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaPulsarContainer extends PulsarContainer implements DevServiceLinkProvider {

    private final PulsarDevServicesProperties properties;

    static final String COMPATIBLE_IMAGE_NAME = "apachepulsar/pulsar";

    public ArconiaPulsarContainer(PulsarDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;

        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isFixedPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), BROKER_PORT);
        }
        if (ContainerUtils.isFixedPort(properties.getAdminPort())) {
            addFixedExposedPort(properties.getAdminPort(), BROKER_HTTP_PORT);
        }
    }

    @Override
    public List<DevServiceLinkDefinition> devServiceLinkDefinitions() {
        return List.of(DevServiceLinkDefinition.builder()
                .id("pulsar")
                .label("Pulsar Admin API")
                .port(BROKER_HTTP_PORT)
                .build());
    }

}
