package io.arconia.docling.client.convert.request.options;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.arconia.core.support.Incubating;

/**
 * OCR engines supported by Docling.
 */
@Incubating(since = "0.15.0")
public enum OcrEngine {

    @JsonProperty("easyocr")
    EASYOCR,
    @JsonProperty("mac")
    OCRMAC,
    @JsonProperty("rapidocr")
    RAPIDOCR,
    @JsonProperty("tesseract")
    TESSEROCR,
    @JsonProperty("tesseract_cloud")
    TESSERACT

}
