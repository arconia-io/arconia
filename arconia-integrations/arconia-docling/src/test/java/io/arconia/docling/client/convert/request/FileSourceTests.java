package io.arconia.docling.client.convert.request;

import org.junit.jupiter.api.Test;

import io.arconia.docling.client.convert.request.source.FileSource;
import io.arconia.docling.client.convert.request.source.Source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FileSource}.
 */
class FileSourceTests {

    @Test
    void whenBase64StringIsNullThenThrow() {
        assertThatThrownBy(() -> new FileSource(Source.Kind.FILE, null, "test.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("base64String cannot be null or empty");
    }

    @Test
    void whenBase64StringIsEmptyThenThrow() {
        assertThatThrownBy(() -> new FileSource(Source.Kind.FILE, "", "test.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("base64String cannot be null or empty");
    }

    @Test
    void whenBase64StringIsBlankThenThrow() {
        assertThatThrownBy(() -> new FileSource(Source.Kind.FILE, "   ", "test.txt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("base64String cannot be null or empty");
    }

    @Test
    void whenFilenameIsNullThenThrow() {
        assertThatThrownBy(() -> new FileSource(Source.Kind.FILE, "dGVzdCBjb250ZW50", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("filename cannot be null or empty");
    }

    @Test
    void whenFilenameIsEmptyThenThrow() {
        assertThatThrownBy(() -> new FileSource(Source.Kind.FILE, "dGVzdCBjb250ZW50", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("filename cannot be null or empty");
    }

    @Test
    void whenFilenameIsBlankThenThrow() {
        assertThatThrownBy(() -> new FileSource(Source.Kind.FILE, "dGVzdCBjb250ZW50", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("filename cannot be null or empty");
    }

    @Test
    void whenValidParametersThenCreateFileSource() {
        String base64String = "dGVzdCBjb250ZW50";
        String filename = "test.txt";

        FileSource fileSource = new FileSource(Source.Kind.FILE, base64String, filename);

        assertThat(fileSource.kind()).isEqualTo(Source.Kind.FILE);
        assertThat(fileSource.base64String()).isEqualTo(base64String);
        assertThat(fileSource.filename()).isEqualTo(filename);
    }

    @Test
    void kindIsAlwaysSetToFile() {
        FileSource fileSource = new FileSource(Source.Kind.HTTP, "dGVzdCBjb250ZW50", "test.txt");

        assertThat(fileSource.kind()).isEqualTo(Source.Kind.FILE);
    }

    @Test
    void fromStaticMethodCreatesFileSource() {
        String filename = "document.pdf";
        String base64String = "dGVzdCBjb250ZW50";

        FileSource fileSource = FileSource.from(filename, base64String);

        assertThat(fileSource.kind()).isEqualTo(Source.Kind.FILE);
        assertThat(fileSource.base64String()).isEqualTo(base64String);
        assertThat(fileSource.filename()).isEqualTo(filename);
    }

    @Test
    void builderCreatesFileSource() {
        String filename = "presentation.pptx";
        String base64String = "dGVzdCBjb250ZW50";

        FileSource fileSource = FileSource.builder()
                .filename(filename)
                .base64String(base64String)
                .build();

        assertThat(fileSource.kind()).isEqualTo(Source.Kind.FILE);
        assertThat(fileSource.base64String()).isEqualTo(base64String);
        assertThat(fileSource.filename()).isEqualTo(filename);
    }

}
