package io.arconia.dev.services.artemis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.artemis.autoconfigure.ArtemisConnectionDetails;
import org.springframework.boot.artemis.autoconfigure.ArtemisMode;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ArtemisDevServicesAutoConfiguration}.
 */
class ArtemisDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    ArtemisDevServicesAutoConfigurationTests() {
        super(ArtemisDevServicesAutoConfiguration.class, "artemis", ArtemisContainer.class, ArtemisConnectionDetails.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new ArtemisDevServicesProperties();
        return asDiscoverableContainer(new ArconiaArtemisContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        ArtemisContainer container = (ArtemisContainer) discoveredContainer;
        ArtemisConnectionDetails connectionDetails = context.getBean(ArtemisConnectionDetails.class);
        assertThat(connectionDetails.getMode()).isEqualTo(ArtemisMode.NATIVE);
        assertThat(connectionDetails.getBrokerUrl()).isEqualTo(container.getBrokerUrl());
        assertThat(connectionDetails.getUser()).isEqualTo(container.getUser());
        assertThat(connectionDetails.getPassword()).isEqualTo(container.getPassword());
    }

    @Test
    void credentialsApplied() {
        getContextRunner()
                .withPropertyValues(
                        "arconia.dev.services.%s.username=myusername".formatted(getServiceName()),
                        "arconia.dev.services.%s.password=mypassword".formatted(getServiceName()))
                .run(context -> {
                    var container = (ArconiaArtemisContainer) context.getBean(getContainerClass());
                    container.configure();
                    assertThat(container.getUser()).isEqualTo("myusername");
                    assertThat(container.getPassword()).isEqualTo("mypassword");
                });
    }

}
