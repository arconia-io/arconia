package io.arconia.docling.client.options;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.docling.client.convert.request.options.PictureDescriptionApi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PictureDescriptionApi}.
 */
class PictureDescriptionApiTests {

    @Test
    void createApiWithAllFields() {
        URI url = URI.create("https://api.example.com/vision");
        Map<String, Object> headers = Map.of("Authorization", "Bearer token123", "Content-Type", "application/json");
        Map<String, Object> params = Map.of("model", "gpt-4-vision", "max_tokens", 100);
        Duration timeout = Duration.ofSeconds(30);
        String prompt = "Describe this image in detail";
        Integer concurrency = 2;

        PictureDescriptionApi api = new PictureDescriptionApi(
                url,
                headers,
                params,
                timeout,
                prompt,
                concurrency
        );

        assertThat(api.url()).isEqualTo(url);
        assertThat(api.headers()).isEqualTo(headers);
        assertThat(api.params()).isEqualTo(params);
        assertThat(api.timeout()).isEqualTo(timeout);
        assertThat(api.prompt()).isEqualTo(prompt);
        assertThat(api.concurrency()).isEqualTo(concurrency);
    }

    @Test
    void createApiWithOnlyRequiredFields() {
        URI url = URI.create("https://api.example.com/vision");

        PictureDescriptionApi api = new PictureDescriptionApi(
                url,
                null,
                null,
                null,
                null,
                null
        );

        assertThat(api.url()).isEqualTo(url);
        assertThat(api.headers()).isNull();
        assertThat(api.params()).isNull();
        assertThat(api.timeout()).isNull();
        assertThat(api.prompt()).isNull();
    }

    @Test
    void createApiWithNullUrlThrowsException() {
        assertThatThrownBy(() -> new PictureDescriptionApi(
                null,
                null,
                null,
                null,
                null,
                null
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("url cannot be null");
    }

    @Test
    void pictureDescriptionApiIsImmutable() {
        URI url = URI.create("https://api.example.com/vision");
        Map<String, Object> headers = new HashMap<>(Map.of("Authorization", "Bearer original"));
        Map<String, Object> params = new HashMap<>(Map.of("model", "original-model"));

        PictureDescriptionApi api = new PictureDescriptionApi(
                url,
                headers,
                params,
                Duration.ofSeconds(10),
                "Original prompt",
                3
        );

        assertThat(api.headers()).isEqualTo(headers);
        assertThat(api.params()).isEqualTo(params);

        headers.put("X-Custom-Header", "modified");
        params.put("temperature", 0.8);

        assertThat(api.headers()).hasSize(1);
        assertThat(api.headers().get("Authorization")).isEqualTo("Bearer original");
        assertThat(api.params()).hasSize(1);
        assertThat(api.params().get("model")).isEqualTo("original-model");
    }

}
