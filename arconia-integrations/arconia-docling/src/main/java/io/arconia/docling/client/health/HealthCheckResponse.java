package io.arconia.docling.client.health;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealthCheckResponse(

    @JsonProperty("status")
    @Nullable
    String status

) {}
