package io.arconia.docling.autoconfigure;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DoclingProperties}.
 */
class DoclingPropertiesTests {

    @Test
    void shouldCreateInstanceWithDefaultValues() {
        DoclingProperties properties = new DoclingProperties();

        assertThat(properties.getBaseUrl()).isEqualTo(URI.create("http://localhost:5001"));
        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void shouldUpdateAllValues() {
        DoclingProperties properties = new DoclingProperties();
        URI newUrl = URI.create("https://custom.docling.com:8080");
        Duration newConnectTimeout = Duration.ofSeconds(15);
        Duration newReadTimeout = Duration.ofSeconds(120);

        properties.setBaseUrl(newUrl);
        properties.setConnectTimeout(newConnectTimeout);
        properties.setReadTimeout(newReadTimeout);

        assertThat(properties.getBaseUrl()).isEqualTo(newUrl);
        assertThat(properties.getConnectTimeout()).isEqualTo(newConnectTimeout);
        assertThat(properties.getReadTimeout()).isEqualTo(newReadTimeout);
    }

}
