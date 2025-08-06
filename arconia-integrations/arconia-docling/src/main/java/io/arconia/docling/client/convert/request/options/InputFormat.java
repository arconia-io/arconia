package io.arconia.docling.client.convert.request.options;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.arconia.core.support.Incubating;

/**
 * A document format supported by document backend parsers.
 */
@Incubating(since = "0.15.0")
public enum InputFormat {

    @JsonProperty("asciidoc")
    ASCIIDOC,
    @JsonProperty("audio")
    AUDIO,
    @JsonProperty("csv")
    CSV,
    @JsonProperty("docx")
    DOCX,
    @JsonProperty("html")
    HTML,
    @JsonProperty("image")
    IMAGE,
    @JsonProperty("json_docling")
    JSON_DOCLING,
    @JsonProperty("md")
    MARKDOWN,
    @JsonProperty("pdf")
    PDF,
    @JsonProperty("pptx")
    PPTX,
    @JsonProperty("xlsx")
    XLSX,
    @JsonProperty("xml_jats")
    XML_JATS,
    @JsonProperty("xml_uspto")
    XML_USPTO

}
