package io.arconia.docling.client.convert.request.options;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OCR engines supported by Docling.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
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
