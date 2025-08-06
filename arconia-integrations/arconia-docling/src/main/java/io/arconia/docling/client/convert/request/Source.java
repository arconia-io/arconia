package io.arconia.docling.client.convert.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.arconia.core.support.Incubating;

/**
 * Source of the document.
 */
@Incubating(since = "0.15.0")
public sealed interface Source permits FileSource, HttpSource {

    enum Kind {

        @JsonProperty("http")
        HTTP,
        @JsonProperty("file")
        FILE

    }

}
