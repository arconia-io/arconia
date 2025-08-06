package io.arconia.docling.client.convert.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.arconia.core.support.Incubating;

/**
 * Target of the document conversion.
 */
@Incubating(since = "0.15.0")
public sealed interface Target permits InBodyTarget, PutTarget, ZipTarget {

    enum Kind {

        @JsonProperty("inbody")
        INBODY,
        @JsonProperty("put")
        PUT,
        @JsonProperty("zip")
        ZIP

    }

}
