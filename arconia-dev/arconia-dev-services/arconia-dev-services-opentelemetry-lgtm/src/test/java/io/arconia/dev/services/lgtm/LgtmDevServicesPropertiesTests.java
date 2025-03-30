package io.arconia.dev.services.lgtm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LgtmDevServicesProperties}.
 */
class LgtmDevServicesPropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(LgtmDevServicesProperties.CONFIG_PREFIX).isEqualTo("arconia.dev.services.lgtm");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        LgtmDevServicesProperties properties = new LgtmDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("grafana/otel-lgtm");
        assertThat(properties.isReusable()).isTrue();
    }

    @Test
    void shouldUpdateValues() {
        LgtmDevServicesProperties properties = new LgtmDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("grafana/otel-lgtm:latest");
        properties.setReusable(false);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("grafana/otel-lgtm:latest");
        assertThat(properties.isReusable()).isFalse();
    }

}
