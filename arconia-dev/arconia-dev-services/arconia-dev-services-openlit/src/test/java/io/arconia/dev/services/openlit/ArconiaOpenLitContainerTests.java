package io.arconia.dev.services.openlit;

import java.util.Map;


import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

/**
 * Unit tests for {@link ArconiaOpenLitContainer}.
 */
class ArconiaOpenLitContainerTests extends BaseDevServiceContainerTests<OpenLitDevServicesProperties, ArconiaOpenLitContainer> {

    @Override
    protected OpenLitDevServicesProperties createProperties() {
        return new OpenLitDevServicesProperties();
    }

    @Override
    protected ArconiaOpenLitContainer createContainer(OpenLitDevServicesProperties properties) {
        return new ArconiaOpenLitContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(OpenLitDevServicesProperties properties) {
        properties.setPort(1234);
        properties.setOtlpGrpcPort(5678);
        properties.setOtlpHttpPort(9067);
        return Map.of(
                1234, ArconiaOpenLitContainer.UI_PORT,
                5678, ArconiaOpenLitContainer.OTLP_GRPC_PORT,
                9067, ArconiaOpenLitContainer.OTLP_HTTP_PORT);
    }

}
