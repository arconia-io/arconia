package io.arconia.docling.client.convert.request.options;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PDF backends supported by Docling.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public enum PdfBackend {

    @JsonProperty("dlparse_v1")
    DLPARSE_V1,
    @JsonProperty("dlparse_v2")
    DLPARSE_V2,
    @JsonProperty("dlparse_v4")
    DLPARSE_V4,
    @JsonProperty("pypdfium2")
    PYPDFIUM2

}
