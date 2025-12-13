package io.arconia.ai.document.docling;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.chunk.request.options.HierarchicalChunkerOptions;
import ai.docling.serve.api.chunk.request.options.HybridChunkerOptions;
import ai.docling.serve.api.chunk.response.Chunk;
import ai.docling.serve.api.chunk.response.ChunkDocumentResponse;
import ai.docling.serve.api.convert.request.options.ConvertDocumentOptions;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DoclingDocumentReader}.
 */
class DoclingDocumentReaderTests {

    private final DoclingServeApi doclingServeApi = mock(DoclingServeApi.class);
    private final DoclingDocumentParser documentParser = mock(DoclingDocumentParser.class);

    @Test
    void builderWhenDoclingServeApiIsNullThenThrow() {
        assertThatThrownBy(() -> DoclingDocumentReader.builder()
                .doclingServeApi(null)
                .files(createMockResource())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("doclingServeApi cannot be null");
    }

    @Test
    void builderWhenConvertOptionsIsNullThenThrow() {
        assertThatThrownBy(() -> DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .convertOptions(null)
                .files(createMockResource())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("convertOptions cannot be null");
    }

    @Test
    void builderWhenChunkerOptionsIsNullThenThrow() {
        assertThatThrownBy(() -> DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .chunkerOptions(null)
                .files(createMockResource())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkerOptions cannot be null");
    }

    @Test
    void builderWhenMetadataIsNullThenThrow() {
        assertThatThrownBy(() -> DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .metadata(null)
                .files(createMockResource())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metadata cannot be null");
    }

    @Test
    void builderWhenFilesIsNullThenThrow() {
        assertThatThrownBy(() -> DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .files((List<Resource>) null)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("files cannot be null");
    }

    @Test
    void builderWhenFilesContainsNullThenThrow() {
        List<Resource> files = new ArrayList<>();
        files.add(null);
        assertThatThrownBy(() -> DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .files(files)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("files cannot contain null elements");
    }

    @Test
    void builderWhenUrlsIsNullThenThrow() {
        assertThatThrownBy(() -> DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .urls((List<URI>) null)
                .files(createMockResource())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("urls cannot be null");
    }

    @Test
    void builderWhenUrlsContainsNullThenThrow() {
        List<URI> urls = new ArrayList<>();
        urls.add(null);
        assertThatThrownBy(() -> DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .urls(urls)
                .files(createMockResource())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("urls cannot contain null elements");
    }

    @Test
    void builderWhenMetadataHasNullKeyThenThrow() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(null, "value");
        assertThatThrownBy(() -> DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .metadata(metadata)
                .files(createMockResource())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metadata cannot contain null keys");
    }

    @Test
    void builderWhenMetadataHasNullValueThenThrow() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", null);
        assertThatThrownBy(() -> DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .metadata(metadata)
                .files(createMockResource())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("metadata cannot contain null values");
    }

    @Test
    void builderWhenNoFilesAndNoUrlsThenThrow() {
        assertThatThrownBy(() -> DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one file or url must be provided");
    }

    @Test
    void builderWithValidFile() {
        DoclingDocumentReader reader = DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .files(createMockResource())
                .build();

        assertThat(reader).isNotNull();
    }

    @Test
    void builderWithValidUrl() {
        DoclingDocumentReader reader = DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .urls("https://example.com/file.pdf")
                .build();

        assertThat(reader).isNotNull();
    }

    @Test
    void builderWithValidFileAndUrl() {
        DoclingDocumentReader reader = DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .files(createMockResource())
                .urls("https://example.com/file.pdf")
                .build();

        assertThat(reader).isNotNull();
    }

    @Test
    void builderWithMultipleFiles() {
        DoclingDocumentReader reader = DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .files(createMockResource(), createMockResource())
                .build();

        assertThat(reader).isNotNull();
    }

    @Test
    void builderWithMultipleUrls() {
        DoclingDocumentReader reader = DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .urls("https://example.com/file1.pdf", "https://example.com/file2.pdf")
                .build();

        assertThat(reader).isNotNull();
    }

    @Test
    void builderWithCustomMetadata() {
        Map<String, Object> metadata = Map.of("source", "test", "version", "1.0");
        DoclingDocumentReader reader = DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .metadata(metadata)
                .files(createMockResource())
                .build();

        assertThat(reader).isNotNull();
    }

    @Test
    void builderWithCustomConvertOptions() {
        ConvertDocumentOptions options = ConvertDocumentOptions.builder().build();
        DoclingDocumentReader reader = DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .convertOptions(options)
                .files(createMockResource())
                .build();

        assertThat(reader).isNotNull();
    }

    @Test
    void builderWithHierarchicalChunkerOptions() {
        DoclingDocumentReader reader = DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .chunkerOptions(HierarchicalChunkerOptions.builder().build())
                .files(createMockResource())
                .build();

        assertThat(reader).isNotNull();
    }

    @Test
    void builderWithHybridChunkerOptions() {
        DoclingDocumentReader reader = DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .chunkerOptions(HybridChunkerOptions.builder().build())
                .files(createMockResource())
                .build();

        assertThat(reader).isNotNull();
    }

    @Test
    void builderWithCustomDocumentParser() {
        DoclingDocumentReader reader = DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .documentParser(documentParser)
                .files(createMockResource())
                .build();

        assertThat(reader).isNotNull();
    }

    @Test
    void whenHierarchicalChunkerThenHierarchicalChunkerMethod() {
        Chunk chunk = createMockChunk();
        List<Chunk> chunks = List.of(chunk);
        ChunkDocumentResponse response = mock(ChunkDocumentResponse.class);
        when(response.getChunks()).thenReturn(chunks);
        when(doclingServeApi.chunkSourceWithHierarchicalChunker(any())).thenReturn(response);

        List<Document> mockDocuments = List.of(createMockDocument());
        when(documentParser.parse(chunks, Map.of())).thenReturn(mockDocuments);

        DoclingDocumentReader reader = DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .chunkerOptions(HierarchicalChunkerOptions.builder().build())
                .documentParser(documentParser)
                .files(createMockResource())
                .build();

        List<Document> documents = reader.get();

        assertThat(documents).isEqualTo(mockDocuments);
    }

    @Test
    void whenHybridChunkerThenHybridChunkerMethod() {
        Chunk chunk = createMockChunk();
        List<Chunk> chunks = List.of(chunk);
        ChunkDocumentResponse response = mock(ChunkDocumentResponse.class);
        when(response.getChunks()).thenReturn(chunks);
        when(doclingServeApi.chunkSourceWithHybridChunker(any())).thenReturn(response);

        List<Document> mockDocuments = List.of(createMockDocument());
        when(documentParser.parse(chunks, Map.of())).thenReturn(mockDocuments);

        DoclingDocumentReader reader = DoclingDocumentReader.builder()
                .doclingServeApi(doclingServeApi)
                .chunkerOptions(HybridChunkerOptions.builder().build())
                .documentParser(documentParser)
                .files(createMockResource())
                .build();

        List<Document> documents = reader.get();

        assertThat(documents).isEqualTo(mockDocuments);
    }

    private Resource createMockResource() {
        Resource resource = mock(Resource.class);
        try {
            when(resource.getFilename()).thenReturn("test.pdf");
            when(resource.getContentAsByteArray()).thenReturn(new byte[] { 1, 2, 3, 4, 5 });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return resource;
    }

    private Chunk createMockChunk() {
        Chunk chunk = mock(Chunk.class);
        when(chunk.getText()).thenReturn("Test content");
        when(chunk.getFilename()).thenReturn("test.pdf");
        return chunk;
    }

    private Document createMockDocument() {
        Document document = mock(Document.class);
        when(document.getId()).thenReturn("doc-id");
        when(document.getText()).thenReturn("Test content");
        return document;
    }

}
