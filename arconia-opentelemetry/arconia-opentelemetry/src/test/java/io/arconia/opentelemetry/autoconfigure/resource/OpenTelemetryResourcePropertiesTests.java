package io.arconia.opentelemetry.autoconfigure.resource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryResourceProperties}.
 */
class OpenTelemetryResourcePropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(OpenTelemetryResourceProperties.CONFIG_PREFIX).isEqualTo("arconia.otel.resource");
    }

    @Test
    void shouldCreateInstanceWithEmptyValues() {
        OpenTelemetryResourceProperties properties = new OpenTelemetryResourceProperties();

        assertThat(properties.getServiceName()).isNull();
        assertThat(properties.getAttributes()).isNotNull().isEmpty();
        assertThat(properties.getEnable()).isNotNull().isEmpty();
    }

    @Test
    void shouldUpdateServiceName() {
        OpenTelemetryResourceProperties properties = new OpenTelemetryResourceProperties();
        String serviceName = "test-service";

        properties.setServiceName(serviceName);

        assertThat(properties.getServiceName()).isEqualTo(serviceName);
    }

    @Test
    void shouldAddAttributes() {
        OpenTelemetryResourceProperties properties = new OpenTelemetryResourceProperties();

        properties.getAttributes().put("key1", "value1");
        properties.getAttributes().put("key2", "value2");

        assertThat(properties.getAttributes())
            .hasSize(2)
            .containsEntry("key1", "value1")
            .containsEntry("key2", "value2");
    }

    @Test
    void shouldConfigureEnabledAttributes() {
        OpenTelemetryResourceProperties properties = new OpenTelemetryResourceProperties();

        properties.getEnable().put("host", true);
        properties.getEnable().put("process", false);

        assertThat(properties.getEnable())
            .hasSize(2)
            .containsEntry("host", true)
            .containsEntry("process", false);
    }

}
