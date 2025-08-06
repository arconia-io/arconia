package io.arconia.docling.client.convert.request.options;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.arconia.core.support.Incubating;

/**
 * PDF backends supported by Docling.
 */
@Incubating(since = "0.15.0")
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
