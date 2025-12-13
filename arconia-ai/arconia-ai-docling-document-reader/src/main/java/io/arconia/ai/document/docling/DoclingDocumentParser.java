package io.arconia.ai.document.docling;

import java.util.List;
import java.util.Map;

import ai.docling.serve.api.chunk.response.Chunk;

import org.springframework.ai.document.Document;

/**
 * Parses a collection of Docling {@link Chunk}s into Spring AI {@link Document}s.
 */
public interface DoclingDocumentParser {

    List<Document> parse(List<Chunk> chunks, Map<String, Object> commonMetadata);

}
