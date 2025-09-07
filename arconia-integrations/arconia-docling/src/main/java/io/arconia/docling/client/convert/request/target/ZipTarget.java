package io.arconia.docling.client.convert.request.target;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Target for zipping the converted document and including it in the response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ZipTarget(@JsonProperty("kind") Target.Kind kind) implements Target {

    public ZipTarget {
        kind = Target.Kind.ZIP;
    }

    public static ZipTarget create() {
        return new ZipTarget(Target.Kind.ZIP);
    }

}
