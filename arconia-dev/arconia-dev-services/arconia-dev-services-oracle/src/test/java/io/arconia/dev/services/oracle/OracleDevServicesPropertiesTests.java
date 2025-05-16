package io.arconia.dev.services.oracle;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OracleDevServicesProperties}.
 */
class OracleDevServicesPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(OracleDevServicesProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.dev.services.oracle");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OracleDevServicesProperties properties = new OracleDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("gvenzl/oracle-free");
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(120));
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.NEVER);
    }

    @Test
    void shouldUpdateValues() {
        OracleDevServicesProperties properties = new OracleDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("gvenzl/oracle-free:latest");
        properties.setStartupTimeout(Duration.ofSeconds(120));
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("gvenzl/oracle-free:latest");
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(120));
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
    }

}
