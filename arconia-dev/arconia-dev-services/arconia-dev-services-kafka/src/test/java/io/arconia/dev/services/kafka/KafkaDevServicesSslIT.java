package io.arconia.dev.services.kafka;

import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleApplicationContextInitializer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.kafka.core.KafkaAdmin;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;
import io.arconia.dev.services.tests.SslTestCertificates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Kafka Dev Service with SSL enabled: the application connects to its
 * listener over SSL through Spring Boot's service connection, with the bundle built from the properties.
 */
@DisabledIfDockerUnavailable
class KafkaDevServicesSslIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KafkaDevServicesAutoConfiguration.class,
                    ServiceConnectionAutoConfiguration.class, KafkaAutoConfiguration.class))
            .withInitializer(new TestcontainersLifecycleApplicationContextInitializer());

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        BootstrapMode.clear();
    }

    @Test
    void applicationConnectsOverSsl() {
        contextRunner
                .withPropertyValues(SslTestCertificates.pemProperties("kafka"))
                .run(context -> {
                    KafkaConnectionDetails connectionDetails = context.getBean(KafkaConnectionDetails.class);
                    assertThat(connectionDetails.getSslBundle()).isNotNull();
                    assertThat(connectionDetails.getSecurityProtocol()).isEqualTo("SSL");

                    KafkaAdmin kafkaAdmin = context.getBean(KafkaAdmin.class);
                    try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                        assertThat(adminClient.describeCluster().clusterId().get(60, TimeUnit.SECONDS)).isNotBlank();
                    }
                });
    }

}
