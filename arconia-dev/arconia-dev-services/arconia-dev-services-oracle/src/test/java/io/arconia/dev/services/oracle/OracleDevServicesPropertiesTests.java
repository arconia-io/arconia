package io.arconia.dev.services.oracle;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

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
        assertThat(properties.getImageName()).isEqualTo("gvenzl/oracle-free:23-slim-faststart");
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.isReusable()).isFalse();
    }

    @Test
    void shouldUpdateValues() {
        OracleDevServicesProperties properties = new OracleDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("gvenzl/oracle-free:latest");
        properties.setStartupTimeout(Duration.ofSeconds(120));
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setReusable(false);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("gvenzl/oracle-free:latest");
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(120));
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.isReusable()).isFalse();
    }

}
