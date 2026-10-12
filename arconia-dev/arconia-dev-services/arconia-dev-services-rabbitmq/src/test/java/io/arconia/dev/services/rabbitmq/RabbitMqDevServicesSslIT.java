package io.arconia.dev.services.rabbitmq;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.amqp.autoconfigure.RabbitConnectionDetails;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleApplicationContextInitializer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;
import io.arconia.dev.services.tests.SslTestCertificates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the RabbitMQ Dev Service with SSL enabled: the application connects to
 * its AMQPS listener through Spring Boot's service connection, with the bundle built from the properties.
 */
@DisabledIfDockerUnavailable
class RabbitMqDevServicesSslIT {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RabbitMqDevServicesAutoConfiguration.class,
                    ServiceConnectionAutoConfiguration.class, RabbitAutoConfiguration.class))
            .withInitializer(new TestcontainersLifecycleApplicationContextInitializer());

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        BootstrapMode.clear();
    }

    @Test
    void applicationConnectsOverSsl() {
        contextRunner
                .withPropertyValues(SslTestCertificates.pemProperties("rabbitmq"))
                .run(context -> {
                    RabbitMQContainer container = context.getBean(RabbitMQContainer.class);
                    RabbitConnectionDetails connectionDetails = context.getBean(RabbitConnectionDetails.class);
                    assertThat(connectionDetails.getSslBundle()).isNotNull();
                    assertThat(connectionDetails.getFirstAddress().port()).isEqualTo(container.getAmqpsPort());

                    try (Connection connection = context.getBean(ConnectionFactory.class).createConnection()) {
                        assertThat(connection.isOpen()).isTrue();
                    }
                });
    }

}
