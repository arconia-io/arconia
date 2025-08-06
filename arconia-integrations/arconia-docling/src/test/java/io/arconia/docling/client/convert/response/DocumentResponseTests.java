package io.arconia.docling.client.convert.response;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DocumentResponse}.
 */
class DocumentResponseTests {

    @Test
    void createResponseWithAllFields() {
        String doctagsContent = "doctags content";
        String filename = "test-document.pdf";
        String htmlContent = "<html><body>Test content</body></html>";
        Map<String, Object> jsonContent = Map.of(
                "title", "Test Document",
                "author", "Test Author",
                "pages", 5
        );
        String markdownContent = "# Test Document\n\nThis is a test document.";
        String textContent = "Test Document\n\nThis is a test document.";

        DocumentResponse response = new DocumentResponse(
                doctagsContent,
                filename,
                htmlContent,
                jsonContent,
                markdownContent,
                textContent
        );

        assertThat(response.doctagsContent()).isEqualTo(doctagsContent);
        assertThat(response.filename()).isEqualTo(filename);
        assertThat(response.htmlContent()).isEqualTo(htmlContent);
        assertThat(response.jsonContent()).isEqualTo(jsonContent);
        assertThat(response.markdownContent()).isEqualTo(markdownContent);
        assertThat(response.textContent()).isEqualTo(textContent);
    }

    @Test
    void createResponseWithNullFields() {
        DocumentResponse response = new DocumentResponse(
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThat(response.doctagsContent()).isNull();
        assertThat(response.filename()).isNull();
        assertThat(response.htmlContent()).isNull();
        assertThat(response.jsonContent()).isNull();
        assertThat(response.markdownContent()).isNull();
        assertThat(response.textContent()).isNull();
    }

    @Test
    void createResponseWithEmptyFields() {
        String filename = "empty-document.txt";
        Map<String, Object> jsonContent = Map.of();
        String markdownContent = "";
        String textContent = "";

        DocumentResponse response = new DocumentResponse(
                null,
                filename,
                null,
                jsonContent,
                markdownContent,
                textContent
        );

        assertThat(response.doctagsContent()).isNull();
        assertThat(response.filename()).isEqualTo(filename);
        assertThat(response.htmlContent()).isNull();
        assertThat(response.jsonContent()).isEmpty();
        assertThat(response.markdownContent()).isEmpty();
        assertThat(response.textContent()).isEmpty();
    }

    @Test
    void documentResponseIsImmutable() {
        Map<String, Object> jsonContent = new HashMap<>(Map.of(
                "original", "value",
                "count", 1
        ));

        DocumentResponse response = new DocumentResponse(
                null,
                null,
                null,
                jsonContent,
                null,
                null
        );

        assertThat(response.jsonContent()).isEqualTo(jsonContent);

        jsonContent.put("modified", "new value");

        assertThat(response.jsonContent()).hasSize(2);
        assertThat(response.jsonContent().get("original")).isEqualTo("value");
        assertThat(response.jsonContent().get("count")).isEqualTo(1);
        assertThat(response.jsonContent()).doesNotContainKey("modified");
    }

}
