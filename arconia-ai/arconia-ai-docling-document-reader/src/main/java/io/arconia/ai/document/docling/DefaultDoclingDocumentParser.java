package io.arconia.ai.document.docling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ai.docling.serve.api.chunk.response.Chunk;

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

            if (chunk.getMetadata().get(DOCLING_ORIGIN) != null && chunk.getMetadata().get(DOCLING_ORIGIN) instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> origin = (Map<String, String>) chunk.getMetadata().get("origin");
                if (origin.get(DOCLING_BINARY_HASH) != null) {
                    metadata.put(DocumentMetadata.BINARY_HASH.value(), origin.get(DOCLING_BINARY_HASH));
                }
                if (origin.get(DOCLING_MIME_TYPE) != null) {
                    metadata.put(DocumentMetadata.MIME_TYPE.value(), origin.get(DOCLING_MIME_TYPE));
                }
                if (origin.get(DOCLING_URI) != null) {
                    metadata.put(DocumentMetadata.URI.value(), origin.get(DOCLING_URI));
                }
            }

            documents.add(Document.builder()
                    .id(UUID.randomUUID().toString())
                    .text(chunk.getText())
                    .metadata(metadata)
                    .build());
        }

        return documents;

    }

}
