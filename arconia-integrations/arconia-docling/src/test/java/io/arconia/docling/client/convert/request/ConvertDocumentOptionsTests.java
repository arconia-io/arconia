package io.arconia.docling.client.convert.request;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.arconia.docling.client.convert.request.options.InputFormat;
import io.arconia.docling.client.convert.request.options.OutputFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ConvertDocumentOptions}.
 */
class ConvertDocumentOptionsTests {

    @Test
    void builder() {
        var options = ConvertDocumentOptions.builder().build();
        assertThat(options).isNotNull();
    }

    @Test
    void convertDocumentOptionsIsImmutable() {
        List<InputFormat> fromFormats = new ArrayList<>(List.of(InputFormat.PDF));
        List<OutputFormat> toFormats = new ArrayList<>(List.of(OutputFormat.MARKDOWN));
        List<String> ocrLang = new ArrayList<>(List.of("en", "de"));

        ConvertDocumentOptions options = ConvertDocumentOptions.builder()
                .fromFormats(fromFormats)
                .toFormats(toFormats)
                .ocrLang(ocrLang)
                .build();

        assertThat(options.fromFormats()).isEqualTo(fromFormats);
        assertThat(options.toFormats()).isEqualTo(toFormats);
        assertThat(options.ocrLang()).isEqualTo(ocrLang);

        fromFormats.add(InputFormat.DOCX);
        toFormats.add(OutputFormat.JSON);
        ocrLang.add("fr");

        assertThat(options.fromFormats()).hasSize(1);
        assertThat(options.fromFormats().get(0)).isEqualTo(InputFormat.PDF);
        assertThat(options.toFormats()).hasSize(1);
        assertThat(options.toFormats().get(0)).isEqualTo(OutputFormat.MARKDOWN);
        assertThat(options.ocrLang()).hasSize(2);
        assertThat(options.ocrLang()).containsExactly("en", "de");
    }

}
