package io.arconia.docling.client.convert.request.options;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.arconia.core.support.Incubating;

/**
 * Modes for the TableFormer model.
 */
@Incubating(since = "0.15.0")
public enum TableFormerMode {

    @JsonProperty("accurate")
    ACCURATE,
    @JsonProperty("fast")
    FAST

}
