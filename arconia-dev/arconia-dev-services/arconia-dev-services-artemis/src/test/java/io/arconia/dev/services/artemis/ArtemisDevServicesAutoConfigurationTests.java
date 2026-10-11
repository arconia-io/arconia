package io.arconia.dev.services.artemis;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.artemis.autoconfigure.ArtemisConnectionDetails;
import org.springframework.boot.artemis.autoconfigure.ArtemisMode;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
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
        return asDiscoverableContainer(ArtemisDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

    @Override
    protected Map<String, Integer> additionalFixedPortProperties() {
        return Map.of("management-console-port", ArtemisDevServicesAutoConfiguration.WEB_CONSOLE_PORT);
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
                    var container = (ArtemisContainer) context.getBean(getContainerClass());
                    // ArtemisContainer applies the credentials to its environment when configured, right before starting.
                    ReflectionTestUtils.invokeMethod(container, "configure");
                    assertThat(container.getUser()).isEqualTo("myusername");
                    assertThat(container.getPassword()).isEqualTo("mypassword");
                });
    }

}
