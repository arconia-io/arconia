package io.arconia.dev.services.rabbitmq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.amqp.autoconfigure.RabbitConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RabbitMqDevServicesAutoConfiguration}.
 */
class RabbitMqDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    RabbitMqDevServicesAutoConfigurationTests() {
        super(RabbitMqDevServicesAutoConfiguration.class, "rabbitmq", RabbitMQContainer.class, RabbitConnectionDetails.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new RabbitMqDevServicesProperties();
        return asDiscoverableContainer(new ArconiaRabbitMqContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        RabbitMQContainer container = (RabbitMQContainer) discoveredContainer;
        RabbitConnectionDetails connectionDetails = context.getBean(RabbitConnectionDetails.class);
        assertThat("amqp://%s:%d".formatted(connectionDetails.getFirstAddress().host(), connectionDetails.getFirstAddress().port()))
                .isEqualTo(container.getAmqpUrl());
        assertThat(connectionDetails.getUsername()).isEqualTo(container.getAdminUsername());
        assertThat(connectionDetails.getPassword()).isEqualTo(container.getAdminPassword());
    }

    @Test
    void credentialsApplied() {
        getContextRunner()
                .withPropertyValues(
                        "arconia.dev.services.%s.username=myusername".formatted(getServiceName()),
                        "arconia.dev.services.%s.password=mypassword".formatted(getServiceName()))
                .run(context -> {
                    var container = (RabbitMQContainer) context.getBean(getContainerClass());
                    assertThat(container.getAdminUsername()).isEqualTo("myusername");
                    assertThat(container.getAdminPassword()).isEqualTo("mypassword");
                });
    }

}
