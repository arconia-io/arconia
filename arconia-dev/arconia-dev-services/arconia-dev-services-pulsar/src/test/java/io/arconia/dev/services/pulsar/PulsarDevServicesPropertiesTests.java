package io.arconia.dev.services.pulsar;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.testcontainers.pulsar.PulsarContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PulsarDevServicesProperties}.
 */
class PulsarDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        PulsarDevServicesProperties properties = new PulsarDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("apachepulsar/pulsar");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getNetworkAliases()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.isShared()).isTrue();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(properties.getManagementConsolePort()).isEqualTo(0);
    }

    @Test
    void shouldUpdateValues() {
        PulsarDevServicesProperties properties = new PulsarDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("apachepulsar/pulsar:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setNetworkAliases(List.of("network1", "network2"));
        properties.setPort(PulsarContainer.BROKER_PORT);
        properties.setShared(false);
        properties.setStartupTimeout(Duration.ofMinutes(1));
        properties.setManagementConsolePort(PulsarContainer.BROKER_HTTP_PORT);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("apachepulsar/pulsar:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getNetworkAliases()).containsExactly("network1", "network2");
        assertThat(properties.getPort()).isEqualTo(PulsarContainer.BROKER_PORT);
        assertThat(properties.isShared()).isFalse();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(1));
        assertThat(properties.getManagementConsolePort()).isEqualTo(ArconiaPulsarContainer.BROKER_HTTP_PORT);
    }

}
