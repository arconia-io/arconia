package io.arconia.docling.client.convert.request.options;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Modes for the TableFormer model.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public enum TableFormerMode {

    @JsonProperty("accurate")
    ACCURATE,
    @JsonProperty("fast")
    FAST

}
