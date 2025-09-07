package io.arconia.docling.client.convert.request.target;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Target of the document conversion.
 */
public sealed interface Target permits InBodyTarget, PutTarget, ZipTarget {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    enum Kind {

        @JsonProperty("inbody")
        INBODY,
        @JsonProperty("put")
        PUT,
        @JsonProperty("zip")
        ZIP

    }

}
