package io.arconia.docling.client.convert.request.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * Represents a file source for a document to convert.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FileSource(

        @JsonProperty("kind")
        Kind kind,

        @JsonProperty("base64_string")
        String base64String,

        @JsonProperty("filename")
        String filename

) implements Source {

    public FileSource {
        Assert.hasText(base64String, "base64String cannot be null or empty");
        Assert.hasText(filename, "filename cannot be null or empty");
        kind = Kind.FILE;
    }

    public static FileSource from(String filename, String base64String) {
        return FileSource.builder()
                .base64String(base64String)
                .filename(filename)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        @Nullable private String base64String;
        @Nullable private String filename;

        private Builder() {}

        /**
         * Content of the file serialized in base64.
         */
        public Builder base64String(String base64String) {
            this.base64String = base64String;
            return this;
        }

        /**
         * Filename of the uploaded document.
         */
        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public FileSource build() {
            return new FileSource(Kind.FILE, base64String, filename);
        }

    }

}
