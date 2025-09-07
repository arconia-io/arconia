package io.arconia.docling.client.convert.request.options;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A document format supported by document backend parsers.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    @JsonProperty("mets_gbs")
    METS_GBS,
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
