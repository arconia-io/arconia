package io.arconia.dev.services.floci;

import java.util.Map;

import io.floci.testcontainers.FlociContainer;

import io.arconia.dev.services.tests.BaseDevServiceContainerTests;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;

/**
 * Unit tests for {@link ArconiaFlociContainer}.
 */
@DisabledIfDockerUnavailable
class ArconiaFlociContainerTests extends BaseDevServiceContainerTests<FlociDevServicesProperties, ArconiaFlociContainer> {

    @Override
    protected FlociDevServicesProperties createProperties() {
        return new FlociDevServicesProperties();
    }

    @Override
    protected ArconiaFlociContainer createContainer(FlociDevServicesProperties properties) {
        return new ArconiaFlociContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(FlociDevServicesProperties properties) {
        properties.setPort(14566);
        return Map.of(14566, FlociContainer.PORT);
    }

}
