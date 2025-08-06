package io.arconia.docling.client.convert.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ConvertDocumentResponse}.
 */
class ConvertDocumentResponseTests {

    @Test
    void createResponseWithAllFields() {
        DocumentResponse document = new DocumentResponse(
                "doctags content",
                "test-file.pdf",
                "<html>content</html>",
                Map.of("key", "value"),
                "# Markdown content",
                "Plain text content"
        );

        List<ErrorItem> errors = List.of(
                new ErrorItem("parser", "Parse error", "pdf_module"),
                new ErrorItem("converter", "Conversion warning", "html_module")
        );

        Double processingTime = 1.5;
        String status = "success";
        Map<String, Object> timings = Map.of(
                "parse_time", 0.8,
                "convert_time", 0.7
        );

        ConvertDocumentResponse response = new ConvertDocumentResponse(
                document,
                errors,
                processingTime,
                status,
                timings
        );

        assertThat(response.document()).isEqualTo(document);
        assertThat(response.errors()).isEqualTo(errors);
        assertThat(response.processingTime()).isEqualTo(processingTime);
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.timings()).isEqualTo(timings);
    }

    @Test
    void createResponseWithNullFields() {
        ConvertDocumentResponse response = new ConvertDocumentResponse(
                null,
                null,
                null,
                null,
                null
        );

        assertThat(response.document()).isNull();
        assertThat(response.errors()).isNull();
        assertThat(response.processingTime()).isNull();
        assertThat(response.status()).isNull();
        assertThat(response.timings()).isNull();
    }

    @Test
    void createResponseWithEmptyCollections() {
        DocumentResponse document = new DocumentResponse(
                null,
                "empty-file.txt",
                null,
                Map.of(),
                null,
                ""
        );

        List<ErrorItem> errors = List.of();
        Map<String, Object> timings = Map.of();

        ConvertDocumentResponse response = new ConvertDocumentResponse(
                document,
                errors,
                0.1,
                "completed",
                timings
        );

        assertThat(response.document()).isEqualTo(document);
        assertThat(response.errors()).isEmpty();
        assertThat(response.processingTime()).isEqualTo(0.1);
        assertThat(response.status()).isEqualTo("completed");
        assertThat(response.timings()).isEmpty();
    }

    @Test
    void convertDocumentResponseIsImmutable() {
        List<ErrorItem> errors = new ArrayList<>(List.of(
                new ErrorItem("original", "Original error", "original_module")
        ));

        Map<String, Object> timings = new HashMap<>(Map.of("original_time", 1.0));

        ConvertDocumentResponse response = new ConvertDocumentResponse(
                null,
                errors,
                null,
                null,
                timings
        );

        assertThat(response.errors()).isEqualTo(errors);
        assertThat(response.timings()).isEqualTo(timings);

        errors.add(new ErrorItem("modified", "Modified error", "modified_module"));
        timings.put("modified_time", 3.0);

        assertThat(response.errors()).hasSize(1);
        assertThat(response.errors().get(0).errorMessage()).isEqualTo("Original error");
        assertThat(response.timings()).hasSize(1);
        assertThat(response.timings().get("original_time")).isEqualTo(1.0);
    }

}
