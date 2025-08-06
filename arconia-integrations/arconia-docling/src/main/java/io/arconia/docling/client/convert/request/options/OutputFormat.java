package io.arconia.docling.client.convert.request.options;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.arconia.core.support.Incubating;

/**
 * Document formats supported for output in Docling.
 */
@Incubating(since = "0.15.0")
public enum OutputFormat {

    @JsonProperty("doctags")
    DOCTAGS,
    @JsonProperty("html")
    HTML,
    @JsonProperty("html_split_page")
    HTML_SPLIT_PAGE,
    @JsonProperty("json")
    JSON,
    @JsonProperty("md")
    MARKDOWN,
    @JsonProperty("text")
    TEXT

}
