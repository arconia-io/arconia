package io.arconia.dev.service.lgtm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LgtmDevServiceProperties}.
 */
class LgtmDevServicePropertiesTests {

    @Test
    void shouldHaveCorrectConfigPrefix() {
        assertThat(LgtmDevServiceProperties.CONFIG_PREFIX).isEqualTo("arconia.dev.services.lgtm");
    }

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        LgtmDevServiceProperties properties = new LgtmDevServiceProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).isEqualTo("grafana/otel-lgtm:0.8.6");
        assertThat(properties.isReusable()).isTrue();
    }

    @Test
    void shouldUpdateValues() {
        LgtmDevServiceProperties properties = new LgtmDevServiceProperties();

        properties.setEnabled(false);
        properties.setImageName("grafana/otel-lgtm:latest");
        properties.setReusable(false);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("grafana/otel-lgtm:latest");
        assertThat(properties.isReusable()).isFalse();
    }

}
