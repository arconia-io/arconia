package io.arconia.docling.client.convert.request.options;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.arconia.core.support.Incubating;

/**
 * Pipelines used by Docling to process documents.
 */
@Incubating(since = "0.15.0")
public enum ProcessingPipeline {

    @JsonProperty("asr")
    ASR,
    @JsonProperty("standard")
    STANDARD,
    @JsonProperty("vlm")
    VLM

}
