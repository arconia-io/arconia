package io.arconia.docling.client.convert.request;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.arconia.docling.client.convert.request.options.ConvertDocumentOptions;
import io.arconia.docling.client.convert.request.source.FileSource;
import io.arconia.docling.client.convert.request.source.HttpSource;
import io.arconia.docling.client.convert.request.target.InBodyTarget;
import io.arconia.docling.client.convert.request.target.ZipTarget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ConvertDocumentRequest}.
 */
class ConvertDocumentRequestTests {

    @Test
    void whenSourcesIsNullThenThrow() {
        assertThatThrownBy(() -> new ConvertDocumentRequest(null, ConvertDocumentOptions.builder().build(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sources cannot be null or empty");
    }

    @Test
    void whenSourcesIsEmptyThenThrow() {
        assertThatThrownBy(() -> new ConvertDocumentRequest(List.of(), ConvertDocumentOptions.builder().build(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sources cannot be null or empty");
    }

    @Test
    void whenOptionsIsNullThenThrow() {
        assertThatThrownBy(() -> new ConvertDocumentRequest(List.of(HttpSource.from("http://example.com")), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("options cannot be null");
    }

    @Test
    void buildWithHttpSourcesAsList() {
        ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                .httpSources(List.of(HttpSource.from("http://example.com")))
                .target(InBodyTarget.create())
                .build();
        assertThat(request.sources()).hasSize(1);
        assertThat(request.sources().get(0)).isInstanceOf(HttpSource.class);
    }

    @Test
    void buildWithHttpSourcesAsVarargs() {
        ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                .addHttpSources(URI.create("http://example.com"), URI.create("http://example.org"))
                .build();
        assertThat(request.sources()).hasSize(2);
        assertThat(request.sources().get(0)).isInstanceOf(HttpSource.class);
        assertThat(request.sources().get(1)).isInstanceOf(HttpSource.class);
    }

    @Test
    void buildWithFileSourcesAsList() {
        ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                .fileSources(List.of(FileSource.from("file:///path/to/file.txt", "content")))
                .build();
        assertThat(request.sources()).hasSize(1);
        assertThat(request.sources().get(0)).isInstanceOf(FileSource.class);
    }

    @Test
    void buildWithFileSourcesAsVarargs() {
        ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                .addFileSources("file1.txt", "base64string1")
                .addFileSources("file2.txt", "base64string2")
                .target(ZipTarget.create())
                .build();
        assertThat(request.sources()).hasSize(2);
        assertThat(request.sources().get(0)).isInstanceOf(FileSource.class);
        assertThat(request.sources().get(1)).isInstanceOf(FileSource.class);
    }

    @Test
    void whenMixedSourcesThenThrow() {
        assertThatThrownBy(() -> ConvertDocumentRequest.builder()
                .httpSources(List.of(HttpSource.from("http://example.com")))
                .addFileSources("file.txt", "base64string")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("All sources must be of the same type (HttpSource or FileSource)");
    }

    @Test
    void convertDocumentRequestIsImmutable() {
        List<FileSource> sources = new ArrayList<>(List.of(FileSource.from("test.txt", "dGVzdCBjb250ZW50")));

        ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                .fileSources(sources)
                .options(ConvertDocumentOptions.builder()
                    .doOcr(true)
                    .build())
                .target(InBodyTarget.create())
                .build();

        assertThat(request.sources()).isEqualTo(sources);

        sources.add(FileSource.from("changed.txt", "Y2hhbmdlZA=="));

        assertThat(request.sources()).hasSize(1);
        FileSource originalFileSource = (FileSource) request.sources().get(0);
        assertThat(originalFileSource.filename()).isEqualTo("test.txt");
        assertThat(originalFileSource.base64String()).isEqualTo("dGVzdCBjb250ZW50");
    }

}
