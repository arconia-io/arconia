package io.arconia.dev.services.docling;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import ai.docling.testcontainers.serve.DoclingServeContainer;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.config.ResourceMapping;

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
        assertThat(properties.getNetworkAliases()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(0);
        assertThat(properties.getResources()).isEmpty();
        assertThat(properties.isShared()).isTrue();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofSeconds(30));

        assertThat(properties.isEnableUi()).isTrue();
        assertThat(properties.getApiKey()).isNull();
    }

    @Test
    void shouldUpdateValues() {
        DoclingDevServicesProperties properties = new DoclingDevServicesProperties();

        properties.setEnabled(false);
        properties.setImageName("ghcr.io/docling-project/docling-serve:latest");
        properties.setEnvironment(Map.of("KEY", "value"));
        properties.setNetworkAliases(List.of("network1", "network2"));
        properties.setPort(DoclingServeContainer.DEFAULT_DOCLING_PORT);
        properties.setResources(List.of(new ResourceMapping("test-resource.txt", "/tmp/test-resource.txt")));
        properties.setShared(false);
        properties.setStartupTimeout(Duration.ofMinutes(1));

        properties.setEnableUi(false);
        properties.setApiKey("caput-draconis");

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getImageName()).isEqualTo("ghcr.io/docling-project/docling-serve:latest");
        assertThat(properties.getEnvironment()).containsEntry("KEY", "value");
        assertThat(properties.getNetworkAliases()).containsExactly("network1", "network2");
        assertThat(properties.getPort()).isEqualTo(DoclingServeContainer.DEFAULT_DOCLING_PORT);
        assertThat(properties.getResources()).hasSize(1);
        assertThat(properties.getResources().getFirst().getSourcePath()).isEqualTo("test-resource.txt");
        assertThat(properties.getResources().getFirst().getContainerPath()).isEqualTo("/tmp/test-resource.txt");
        assertThat(properties.isShared()).isFalse();
        assertThat(properties.getStartupTimeout()).isEqualTo(Duration.ofMinutes(1));

        assertThat(properties.isEnableUi()).isFalse();
        assertThat(properties.getApiKey()).isEqualTo("caput-draconis");
    }

}
