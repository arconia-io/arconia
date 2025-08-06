package io.arconia.docling.client.convert.request;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link HttpSource}.
 */
class HttpSourceTests {

    @Test
    void whenUrlIsNullThenThrow() {
        assertThatThrownBy(() -> new HttpSource(Source.Kind.HTTP, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url cannot be null");
    }

    @Test
    void whenValidParametersThenCreateHttpSource() {
        URI url = URI.create("https://example.com/document.pdf");
        Map<String, Object> headers = Map.of("Authorization", "Bearer token123");

        HttpSource httpSource = new HttpSource(Source.Kind.HTTP, url, headers);

        assertThat(httpSource.kind()).isEqualTo(Source.Kind.HTTP);
        assertThat(httpSource.url()).isEqualTo(url);
        assertThat(httpSource.headers()).isEqualTo(headers);
    }

    @Test
    void whenRequiredParametersThenCreateHttpSource() {
        URI url = URI.create("https://example.com/document.pdf");

        HttpSource httpSource = new HttpSource(Source.Kind.HTTP, url, null);

        assertThat(httpSource.kind()).isEqualTo(Source.Kind.HTTP);
        assertThat(httpSource.url()).isEqualTo(url);
        assertThat(httpSource.headers()).isNull();
    }

    @Test
    void kindIsAlwaysSetToHttp() {
        URI url = URI.create("https://example.com/document.pdf");
        HttpSource httpSource = new HttpSource(Source.Kind.FILE, url, null);

        assertThat(httpSource.kind()).isEqualTo(Source.Kind.HTTP);
    }

    @Test
    void fromStaticMethodWithStringCreatesHttpSource() {
        String urlString = "https://example.com/document.pdf";

        HttpSource httpSource = HttpSource.from(urlString);

        assertThat(httpSource.kind()).isEqualTo(Source.Kind.HTTP);
        assertThat(httpSource.url()).isEqualTo(URI.create(urlString));
        assertThat(httpSource.headers()).isNull();
    }

    @Test
    void fromStaticMethodWithUriCreatesHttpSource() {
        URI url = URI.create("https://example.com/document.pdf");

        HttpSource httpSource = HttpSource.from(url);

        assertThat(httpSource.kind()).isEqualTo(Source.Kind.HTTP);
        assertThat(httpSource.url()).isEqualTo(url);
        assertThat(httpSource.headers()).isNull();
    }

    @Test
    void builderCreatesHttpSource() {
        URI url = URI.create("https://example.com/presentation.pptx");
        Map<String, Object> headers = Map.of("User-Agent", "test-agent");

        HttpSource httpSource = HttpSource.builder()
                .url(url)
                .headers(headers)
                .build();

        assertThat(httpSource.kind()).isEqualTo(Source.Kind.HTTP);
        assertThat(httpSource.url()).isEqualTo(url);
        assertThat(httpSource.headers()).isEqualTo(headers);
    }

    @Test
    void httpSourceIsImmutable() {
        Map<String, Object> headers = new HashMap<>(Map.of("Authorization", "Bearer token123"));

        HttpSource httpSource = HttpSource.builder()
                .url(URI.create("https://example.com/test.pdf"))
                .headers(headers)
                .build();

        assertThat(httpSource.headers()).isEqualTo(headers);

        headers.put("newStuff", "awesome");

        assertThat(httpSource.headers()).size().isEqualTo(1);
        assertThat(httpSource.headers()).containsEntry("Authorization", "Bearer token123");
        assertThat(httpSource.headers()).doesNotContainKey("newStuff");
    }

}
