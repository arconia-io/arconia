package io.arconia.docling.client.convert.request.options;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Pipelines used by Docling to process documents.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public enum ProcessingPipeline {

    @JsonProperty("asr")
    ASR,
    @JsonProperty("standard")
    STANDARD,
    @JsonProperty("vlm")
    VLM

}
