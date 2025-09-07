package io.arconia.docling.client.convert.request.options;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The mode for how to handle image references in the document.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public enum ImageRefMode {

    @JsonProperty("embedded")
    EMBEDDED,
    @JsonProperty("placeholder")
    PLACEHOLDER,
    @JsonProperty("referenced")
    REFERENCED

}
