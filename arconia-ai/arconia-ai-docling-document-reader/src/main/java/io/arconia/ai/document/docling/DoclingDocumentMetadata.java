package io.arconia.ai.document.docling;

import org.springframework.ai.document.Document;

/**
 * Metadata keys of the {@link Document}s created by the {@link DoclingDocumentReader}.
 */
public enum DoclingDocumentMetadata {

    BINARY_HASH("binary_hash"),
    FILE_NAME("file_name"),
    HEADINGS("headings"),
    MIME_TYPE("mime_type"),
    PAGE_NUMBERS("page_numbers"),
    URI("uri");

    private final String value;

    DoclingDocumentMetadata(String value) {
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
