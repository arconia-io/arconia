package io.arconia.docling.client.convert.request.options;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.arconia.core.support.Incubating;

/**
 * The mode for how to handle image references in the document.
 */
@Incubating(since = "0.15.0")
public enum ImageRefMode {

    @JsonProperty("embedded")
    EMBEDDED,
    @JsonProperty("placeholder")
    PLACEHOLDER,
    @JsonProperty("referenced")
    REFERENCED

}
