package io.arconia.dev.services.docling;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.core.config.DevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DoclingDevServicesProperties}.
 */
class DoclingDevServicesPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        DoclingDevServicesProperties properties = new DoclingDevServicesProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getImageName()).contains("ghcr.io/docling-project/docling-serve");
        assertThat(properties.getEnvironment()).isEmpty();
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.DEV_MODE);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(properties.isEnableUi()).isTrue();
    }

    @Test
    void shouldUpdateValues() {
        DoclingDevServicesProperties properties = new DoclingDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("ghcr.io/docling-project/docling-serve:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setShared(DevServicesProperties.Shared.ALWAYS);
        properties.setStartupTimeout(Duration.ofMinutes(5));
        properties.setEnableUi(false);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("ghcr.io/docling-project/docling-serve:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getShared()).isEqualTo(DevServicesProperties.Shared.ALWAYS);
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(5));
        assertThat(properties.isEnableUi()).isFalse();
    }

}
