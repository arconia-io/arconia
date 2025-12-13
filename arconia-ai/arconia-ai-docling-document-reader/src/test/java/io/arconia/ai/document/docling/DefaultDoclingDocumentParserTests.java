package io.arconia.ai.document.docling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.docling.serve.api.chunk.response.Chunk;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultDoclingDocumentParser}.
 */
class DefaultDoclingDocumentParserTests {

    private final DefaultDoclingDocumentParser parser = new DefaultDoclingDocumentParser();

    @Test
    void whenChunksIsNullThenThrow() {
        assertThatThrownBy(() -> parser.parse(null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunks cannot be null");
    }

    @Test
    void whenMetadataIsNullThenThrow() {
        assertThatThrownBy(() -> parser.parse(List.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("commonMetadata cannot be null");
    }

    @Test
    void whenMetadataHaveNullKeyThenThrow() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(null, "value");
        assertThatThrownBy(() -> parser.parse(List.of(), metadata))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("commonMetadata cannot contain null keys");
    }

    @Test
    void whenMetadataHaveNullValueThenThrow() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", null);
        assertThatThrownBy(() -> parser.parse(List.of(), metadata))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("commonMetadata cannot contain null values");
    }

    @Test
    void shouldParseChunkWithAllMetadataFields() {
        Chunk chunk = createChunkWithAllMetadata();
        List<Chunk> chunks = List.of(chunk);
        Map<String, Object> commonMetadata = Map.of("source", "test");

        List<Document> documents = parser.parse(chunks, commonMetadata);

        assertThat(documents).hasSize(1);
        Document document = documents.getFirst();
        assertThat(document.getText()).isEqualTo("Test content");
        assertThat(document.getId()).isNotNull();
        assertThat(document.getMetadata()).containsEntry("file_name", "test.pdf");
        assertThat(document.getMetadata()).containsEntry("headings", List.of("Heading 1", "Heading 2"));
        assertThat(document.getMetadata()).containsEntry("page_numbers", List.of(1, 2, 3));
        assertThat(document.getMetadata()).containsEntry("binary_hash", "abc123");
        assertThat(document.getMetadata()).containsEntry("mime_type", "application/pdf");
        assertThat(document.getMetadata()).containsEntry("uri", "https://example.com/test.pdf");
        assertThat(document.getMetadata()).containsEntry("source", "test");
    }

    @Test
    void shouldParseChunkWithoutHeadings() {
        Chunk chunk = mock(Chunk.class);
        when(chunk.getText()).thenReturn("Test content");
        when(chunk.getFilename()).thenReturn("test.pdf");
        when(chunk.getHeadings()).thenReturn(List.of());

        List<Document> documents = parser.parse(List.of(chunk), new HashMap<>());

        assertThat(documents).hasSize(1);
        Document document = documents.getFirst();
        assertThat(document.getMetadata()).doesNotContainKey("headings");
    }

    @Test
    void shouldParseChunkWithoutPageNumbers() {
        Chunk chunk = mock(Chunk.class);
        when(chunk.getText()).thenReturn("Test content");
        when(chunk.getFilename()).thenReturn("test.pdf");
        when(chunk.getPageNumbers()).thenReturn(List.of());

        List<Document> documents = parser.parse(List.of(chunk), new HashMap<>());

        assertThat(documents).hasSize(1);
        Document document = documents.getFirst();
        assertThat(document.getMetadata()).doesNotContainKey("page_numbers");
    }

    @Test
    void shouldParseChunkWithoutOriginMetadata() {
        Chunk chunk = mock(Chunk.class);
        when(chunk.getText()).thenReturn("Test content");
        when(chunk.getFilename()).thenReturn("test.pdf");
        when(chunk.getMetadata()).thenReturn(new HashMap<>());

        List<Document> documents = parser.parse(List.of(chunk), new HashMap<>());

        assertThat(documents).hasSize(1);
        Document document = documents.getFirst();
        assertThat(document.getMetadata()).doesNotContainKeys("binary_hash", "mime_type", "uri");
    }

    @Test
    void shouldParseChunkWithOriginMetadataButMissingFields() {
        Chunk chunk = mock(Chunk.class);
        when(chunk.getText()).thenReturn("Test content");
        when(chunk.getFilename()).thenReturn("test.pdf");
        Map<String, Object> metadata = new HashMap<>();
        Map<String, String> origin = new HashMap<>();
        origin.put("binary_hash", "abc123");
        metadata.put("origin", origin);
        when(chunk.getMetadata()).thenReturn(metadata);

        List<Document> documents = parser.parse(List.of(chunk), new HashMap<>());

        assertThat(documents).hasSize(1);
        Document document = documents.getFirst();
        assertThat(document.getMetadata())
                .containsEntry("binary_hash", "abc123")
                .doesNotContainKeys("mime_type", "uri");
    }

    @Test
    void shouldParseChunkWithNullOriginMetadata() {
        Chunk chunk = mock(Chunk.class);
        when(chunk.getText()).thenReturn("Test content");
        when(chunk.getFilename()).thenReturn("test.pdf");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("origin", null);
        when(chunk.getMetadata()).thenReturn(metadata);

        List<Document> documents = parser.parse(List.of(chunk), new HashMap<>());

        assertThat(documents).hasSize(1);
        Document document = documents.getFirst();
        assertThat(document.getMetadata())
                .doesNotContainKeys("binary_hash", "mime_type", "uri")
                .containsEntry("file_name", "test.pdf");
    }

    @Test
    void shouldParseChunkWithOriginMetadataAsNonMapObject() {
        Chunk chunk = mock(Chunk.class);
        when(chunk.getText()).thenReturn("Test content");
        when(chunk.getFilename()).thenReturn("test.pdf");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("origin", "not a map");
        when(chunk.getMetadata()).thenReturn(metadata);

        List<Document> documents = parser.parse(List.of(chunk), new HashMap<>());

        assertThat(documents).hasSize(1);
        Document document = documents.getFirst();
        assertThat(document.getMetadata())
                .doesNotContainKeys("binary_hash", "mime_type", "uri")
                .containsEntry("file_name", "test.pdf");
    }

    @Test
    void shouldParseEmptyChunkList() {
        List<Document> documents = parser.parse(new ArrayList<>(), Map.of("source", "test"));

        assertThat(documents).isEmpty();
    }

    @Test
    void shouldGenerateUniqueDocumentIds() {
        Chunk chunk1 = createChunkWithAllMetadata();
        Chunk chunk2 = createChunkWithAllMetadata();

        List<Document> documents = parser.parse(List.of(chunk1, chunk2), new HashMap<>());

        Set<String> ids = new HashSet<>();
        for (Document document : documents) {
            ids.add(document.getId());
        }
        assertThat(ids).hasSize(2);
    }

    @Test
    void shouldPreserveCommonMetadata() {
        Chunk chunk = mock(Chunk.class);
        when(chunk.getText()).thenReturn("Test content");
        when(chunk.getFilename()).thenReturn("test.pdf");
        when(chunk.getMetadata()).thenReturn(new HashMap<>());

        Map<String, Object> commonMetadata = new HashMap<>();
        commonMetadata.put("source", "test-source");
        commonMetadata.put("version", "1.0");
        commonMetadata.put("category", "documents");

        List<Document> documents = parser.parse(List.of(chunk), commonMetadata);

        assertThat(documents).hasSize(1);
        Document document = documents.getFirst();
        assertThat(document.getMetadata())
                .containsEntry("source", "test-source")
                .containsEntry("version", "1.0")
                .containsEntry("category", "documents")
                .containsEntry("file_name", "test.pdf");
    }

    private Chunk createChunkWithAllMetadata() {
        Chunk chunk = mock(Chunk.class);
        when(chunk.getText()).thenReturn("Test content");
        when(chunk.getFilename()).thenReturn("test.pdf");
        when(chunk.getHeadings()).thenReturn(List.of("Heading 1", "Heading 2"));
        when(chunk.getPageNumbers()).thenReturn(List.of(1, 2, 3));
        when(chunk.getMetadata()).thenReturn(createOriginMetadata());
        return chunk;
    }

    private Map<String, Object> createOriginMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        Map<String, String> origin = new HashMap<>();
        origin.put("binary_hash", "abc123");
        origin.put("mimetype", "application/pdf");
        origin.put("uri", "https://example.com/test.pdf");
        metadata.put("origin", origin);
        return metadata;
    }

}
