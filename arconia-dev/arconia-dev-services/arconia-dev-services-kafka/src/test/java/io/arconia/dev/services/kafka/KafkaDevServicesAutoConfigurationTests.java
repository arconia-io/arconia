package io.arconia.dev.services.kafka;

import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link KafkaDevServicesAutoConfiguration}.
 */
class KafkaDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    KafkaDevServicesAutoConfigurationTests() {
        super(KafkaDevServicesAutoConfiguration.class, "kafka", KafkaContainer.class, KafkaConnectionDetails.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new KafkaDevServicesProperties();
        return asDiscoverableContainer(new ArconiaKafkaContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        KafkaContainer container = (KafkaContainer) discoveredContainer;
        KafkaConnectionDetails connectionDetails = context.getBean(KafkaConnectionDetails.class);
        assertThat(connectionDetails.getBootstrapServers()).containsExactly(container.getBootstrapServers());
        assertThat(connectionDetails.getSecurityProtocol()).isEqualTo("PLAINTEXT");
    }

}
