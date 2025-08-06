package io.arconia.docling.client.convert.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.arconia.core.support.Incubating;

@Incubating(since = "0.15.0")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorItem(

        @JsonProperty("component_type")
        String componentType,

        @JsonProperty("error_message")
        String errorMessage,

        @JsonProperty("module_name")
        String moduleName

) {}
