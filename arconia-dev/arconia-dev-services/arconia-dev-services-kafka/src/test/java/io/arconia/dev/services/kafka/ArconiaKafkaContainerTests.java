package io.arconia.dev.services.kafka;

import java.util.Map;


import io.arconia.dev.services.tests.BaseDevServiceContainerTests;

/**
 * Unit tests for {@link ArconiaKafkaContainer}.
 */
class ArconiaKafkaContainerTests extends BaseDevServiceContainerTests<KafkaDevServicesProperties, ArconiaKafkaContainer> {

    @Override
    protected KafkaDevServicesProperties createProperties() {
        return new KafkaDevServicesProperties();
    }

    @Override
    protected ArconiaKafkaContainer createContainer(KafkaDevServicesProperties properties) {
        return new ArconiaKafkaContainer(properties);
    }

    @Override
    protected Map<Integer, Integer> configureFixedPorts(KafkaDevServicesProperties properties) {
        properties.setPort(1234);
        return Map.of(1234, ArconiaKafkaContainer.KAFKA_PORT);
    }

}
