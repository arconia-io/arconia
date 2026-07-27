package io.arconia.dev.services.openlit;

import java.util.List;

import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.util.ContainerUtils;
import io.arconia.testcontainers.openlit.OpenLitContainer;

/**
 * An {@link OpenLitContainer} configured for use with Arconia Dev Services.
 */
final class ArconiaOpenLitContainer extends OpenLitContainer implements DevServiceLinkProvider {

    static final String COMPATIBLE_IMAGE_NAME = "ghcr.io/openlit/openlit";

    private final OpenLitDevServicesProperties properties;

    ArconiaOpenLitContainer(OpenLitDevServicesProperties properties) {
        super(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        this.properties = properties;
        this.withClickHouseImage(properties.getClickhouseImageName());
        ContainerConfigurer.base(this, properties);
    }

    @Override
    protected void configure() {
        super.configure();
        if (ContainerUtils.isFixedPort(properties.getPort())) {
            addFixedExposedPort(properties.getPort(), UI_PORT);
        }
        if (ContainerUtils.isFixedPort(properties.getOtlpGrpcPort())) {
            addFixedExposedPort(properties.getOtlpGrpcPort(), OTLP_GRPC_PORT);
        }
        if (ContainerUtils.isFixedPort(properties.getOtlpHttpPort())) {
            addFixedExposedPort(properties.getOtlpHttpPort(), OTLP_HTTP_PORT);
        }
    }

    @Override
    public List<DevServiceLink> devServiceLinks() {
        return List.of(DevServiceLink.builder()
                .id("openlit")
                .label("OpenLit UI")
                .url(getOpenLitUrl())
                .build());
    }

}
