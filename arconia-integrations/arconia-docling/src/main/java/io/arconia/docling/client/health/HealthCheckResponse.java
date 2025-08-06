package io.arconia.docling.client.health;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jspecify.annotations.Nullable;

import io.arconia.core.support.Incubating;

@Incubating(since = "0.15.0")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealthCheckResponse(

    @JsonProperty("status")
    @Nullable
    String status

) {}
