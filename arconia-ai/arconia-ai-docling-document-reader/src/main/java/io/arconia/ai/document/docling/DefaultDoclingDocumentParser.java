package io.arconia.ai.document.docling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.docling.serve.api.chunk.response.Chunk;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Default parser for converting Docling document {@link Chunk}s into Spring AI {@link Document}s.
 */
public final class DefaultDoclingDocumentParser implements DoclingDocumentParser {

    private static final String DOCLING_BINARY_HASH = "binary_hash";
    private static final String DOCLING_MIME_TYPE = "mimetype";
    private static final String DOCLING_ORIGIN =  "origin";
    private static final String DOCLING_URI = "uri";

    @Override
    public List<Document> parse(List<Chunk> chunks, Map<String, Object> commonMetadata) {
        Assert.notNull(chunks, "chunks cannot be null");
        Assert.notNull(commonMetadata, "commonMetadata cannot be null");
        Assert.noNullElements(commonMetadata.keySet(), "commonMetadata cannot contain null keys");
        Assert.noNullElements(commonMetadata.values(), "commonMetadata cannot contain null values");

        List<Document> documents = new ArrayList<>();

        for (Chunk chunk : chunks) {
            Map<String, Object> metadata = new HashMap<>(commonMetadata);

            metadata.put(DocumentMetadata.FILE_NAME.value(), chunk.getFilename());
            if (!CollectionUtils.isEmpty(chunk.getHeadings())) {
                metadata.put(DocumentMetadata.HEADINGS.value(), chunk.getHeadings());
            }
            if (!CollectionUtils.isEmpty(chunk.getPageNumbers())) {
                metadata.put(DocumentMetadata.PAGE_NUMBERS.value(), chunk.getPageNumbers());
            }

            if (chunk.getMetadata().get(DOCLING_ORIGIN) instanceof Map<?, ?> origin) {
                putIfPresent(metadata, DocumentMetadata.BINARY_HASH, origin.get(DOCLING_BINARY_HASH));
                putIfPresent(metadata, DocumentMetadata.MIME_TYPE, origin.get(DOCLING_MIME_TYPE));
                putIfPresent(metadata, DocumentMetadata.URI, origin.get(DOCLING_URI));
            }

            documents.add(Document.builder()
                    .text(chunk.getText())
                    .metadata(metadata)
                    .build());
        }

        return documents;
    }

    private static void putIfPresent(Map<String, Object> metadata, DocumentMetadata key, @Nullable Object value) {
        if (value != null) {
            metadata.put(key.value(), value);
        }
    }

}
