package io.arconia.docling.client.convert.request.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Source of the document.
 */
public sealed interface Source permits FileSource, HttpSource {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    enum Kind {

        @JsonProperty("http")
        HTTP,
        @JsonProperty("file")
        FILE

    }

}
