package io.arconia.dev.services.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;
import io.arconia.dev.services.tests.DevServiceContainers;
import io.arconia.dev.services.tests.SslTestCertificates;

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
        return asDiscoverableContainer(KafkaDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        KafkaContainer container = (KafkaContainer) discoveredContainer;
        KafkaConnectionDetails connectionDetails = context.getBean(KafkaConnectionDetails.class);
        assertThat(connectionDetails.getBootstrapServers()).containsExactly(container.getBootstrapServers());
        assertThat(connectionDetails.getSecurityProtocol()).isEqualTo("PLAINTEXT");
    }

    @Test
    void sslApplied() {
        getContextRunner()
                .withPropertyValues(SslTestCertificates.pemProperties(getServiceName()))
                .run(context -> {
                    var container = (KafkaContainer) context.getBean(getContainerClass());
                    // The listener published to the host keeps its name and switches to SSL; the internal ones stay in plaintext.
                    assertThat(container.getEnvMap())
                            .containsEntry("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:SSL,CONTROLLER:PLAINTEXT")
                            .containsEntry("KAFKA_SSL_KEYSTORE_TYPE", "PEM")
                            .containsEntry("KAFKA_SSL_KEYSTORE_LOCATION", KafkaDevServicesAutoConfiguration.SSL_DIRECTORY + "keystore.pem")
                            .containsEntry("KAFKA_SSL_TRUSTSTORE_TYPE", "PEM")
                            .containsEntry("KAFKA_SSL_TRUSTSTORE_LOCATION", KafkaDevServicesAutoConfiguration.SSL_DIRECTORY + "truststore.pem")
                            .containsEntry("KAFKA_SSL_CLIENT_AUTH", "none");
                    assertThat(DevServiceContainers.copiedFiles(container)).containsValues(
                            KafkaDevServicesAutoConfiguration.SSL_DIRECTORY + "keystore.pem",
                            KafkaDevServicesAutoConfiguration.SSL_DIRECTORY + "truststore.pem");
                });
    }

}
