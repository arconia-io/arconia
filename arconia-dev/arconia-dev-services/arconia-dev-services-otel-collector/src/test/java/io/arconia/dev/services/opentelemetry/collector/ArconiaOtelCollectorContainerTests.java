package io.arconia.dev.services.opentelemetry.collector;

import java.util.Map;


import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

/**
 * Unit tests for {@link ArconiaOtelCollectorContainer}.
 */
class ArconiaOtelCollectorContainerTests extends BaseDevServiceContainerTests<OtelCollectorDevServicesProperties, ArconiaOtelCollectorContainer> {

    @Override
    protected OtelCollectorDevServicesProperties createProperties() {
        return new OtelCollectorDevServicesProperties();
    }

    @Override
    protected ArconiaOtelCollectorContainer createContainer(OtelCollectorDevServicesProperties properties) {
        return new ArconiaOtelCollectorContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(OtelCollectorDevServicesProperties properties) {
        properties.setPort(1234);
        properties.setOtlpGrpcPort(5678);
        return Map.of(
                1234, ArconiaOtelCollectorContainer.OTLP_HTTP_PORT,
                5678, ArconiaOtelCollectorContainer.OTLP_GRPC_PORT);
    }

}
