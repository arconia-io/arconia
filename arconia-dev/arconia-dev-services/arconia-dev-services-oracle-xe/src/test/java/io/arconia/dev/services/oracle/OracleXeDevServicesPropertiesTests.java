package io.arconia.dev.services.oracle;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OracleXeDevServicesProperties}.
 */
class OracleXeDevServicesPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(OracleXeDevServicesProperties.CONFIG_PREFIX)
                .isEqualTo("arconia.dev.services.oracle-xe");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        OracleXeDevServicesProperties properties = new OracleXeDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).isEqualTo("gvenzl/oracle-xe:21-slim-faststart");
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.isReusable()).isFalse();
    }

    @Test
    void shouldUpdateValues() {
        OracleXeDevServicesProperties properties = new OracleXeDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("gvenzl/oracle-xe:latest");
        properties.setStartupTimeout(Duration.ofSeconds(120));
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setReusable(false);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("gvenzl/oracle-xe:latest");
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(120));
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.isReusable()).isFalse();
    }

}
