package io.arconia.docling.client.convert.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConvertDocumentResponse(

        @JsonProperty("document")
        DocumentResponse document,

        @JsonProperty("errors")
        @Nullable
        List<ErrorItem> errors,

        @JsonProperty("processing_time")
        Double processingTime,

        @JsonProperty("status")
        String status,

        @JsonProperty("timings")
        @Nullable
        Map<String, Object> timings

) {

    public ConvertDocumentResponse {
        if (errors != null) {
            errors = new ArrayList<>(errors);
        }
        if (timings != null) {
            timings = new HashMap<>(timings);
        }
    }

}
