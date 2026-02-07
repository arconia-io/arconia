package io.arconia.dev.services.artemis;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.config.ResourceMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ArtemisDevServicesProperties}.
 */
class ArtemisDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        ArtemisDevServicesProperties properties = new ArtemisDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("apache/activemq-artemis");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getNetworkAliases()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getResources()).isEmpty();
        assertThat(properties.isShared()).isTrue();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(30));

        assertThat(properties.getManagementConsolePort()).isEqualTo(0);
        assertThat(properties.getUsername()).isEqualTo(ArtemisDevServicesProperties.DEFAULT_USERNAME);
        assertThat(properties.getPassword()).isEqualTo(ArtemisDevServicesProperties.DEFAULT_PASSWORD);
    }

    @Test
    void shouldUpdateValues() {
        ArtemisDevServicesProperties properties = new ArtemisDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("apache/activemq-artemis:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setNetworkAliases(List.of("network1", "network2"));
        properties.setPort(ArconiaArtemisContainer.TCP_PORT);
        properties.setResources(List.of(new ResourceMapping("test-resource.txt", "/tmp/test-resource.txt")));
        properties.setShared(false);
        properties.setStartupTimeout(Duration.ofMinutes(1));

        properties.setManagementConsolePort(ArconiaArtemisContainer.WEB_CONSOLE_PORT);
        properties.setUsername("myusername");
        properties.setPassword("mypassword");

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("apache/activemq-artemis:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getNetworkAliases()).containsExactly("network1", "network2");
        assertThat(properties.getPort()).isEqualTo(ArconiaArtemisContainer.TCP_PORT);
        assertThat(properties.getResources()).hasSize(1);
        assertThat(properties.getResources().getFirst().getSourcePath()).isEqualTo("test-resource.txt");
        assertThat(properties.getResources().getFirst().getContainerPath()).isEqualTo("/tmp/test-resource.txt");
        assertThat(properties.isShared()).isFalse();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(1));

        assertThat(properties.getManagementConsolePort()).isEqualTo(ArconiaArtemisContainer.WEB_CONSOLE_PORT);
        assertThat(properties.getUsername()).isEqualTo("myusername");
        assertThat(properties.getPassword()).isEqualTo("mypassword");
    }

}
