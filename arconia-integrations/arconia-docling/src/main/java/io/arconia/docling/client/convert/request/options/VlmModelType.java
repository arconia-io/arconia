package io.arconia.docling.client.convert.request.options;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Local and API models for the VLM pipeline.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public enum VlmModelType {

    @JsonProperty("smoldocling")
    SMOLDOCLING,
    @JsonProperty("smoldocling_vllm")
    SMOLDOCLING_VLLM,
    @JsonProperty("granite_vision")
    GRANITE_VISION,
    @JsonProperty("granite_vision_vllm")
    GRANITE_VISION_VLLM,
    @JsonProperty("granite_vision_ollama")
    GRANITE_VISION_OLLAMA,
    @JsonProperty("got_ocr_2")
    GOT_OCR_2

}
