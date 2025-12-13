package io.arconia.ai.document.docling;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.document.DocumentTransformer;

/**
 * Common set of metadata keys used in a {@link Document} by a {@link DocumentReader}
 * or a {@link DocumentTransformer}.
 */
public enum DocumentMetadata {

    BINARY_HASH("binary_hash"),
    FILE_NAME("file_name"),
    HEADINGS("headings"),
    MIME_TYPE("mime_type"),
    PAGE_NUMBERS("page_numbers"),
    URI("uri");

    private final String value;

    DocumentMetadata(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.value;
    }

}
