package io.arconia.dev.services.phoenix;

import java.util.Map;

import io.arconia.dev.services.tests.BaseDevServiceContainerTests;
import io.arconia.testcontainers.phoenix.PhoenixContainer;

/**
 * Unit tests for {@link ArconiaPhoenixContainer}.
 */
class ArconiaPhoenixContainerTests extends BaseDevServiceContainerTests<PhoenixDevServicesProperties, ArconiaPhoenixContainer> {

    @Override
    protected PhoenixDevServicesProperties createProperties() {
        return new PhoenixDevServicesProperties();
    }

    @Override
    protected ArconiaPhoenixContainer createContainer(PhoenixDevServicesProperties properties) {
        return new ArconiaPhoenixContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(PhoenixDevServicesProperties properties) {
        properties.setPort(1234);
        properties.setOtlpGrpcPort(5678);
        return Map.of(
                1234, PhoenixContainer.HTTP_PORT,
                5678, PhoenixContainer.GRPC_PORT);
    }

}
