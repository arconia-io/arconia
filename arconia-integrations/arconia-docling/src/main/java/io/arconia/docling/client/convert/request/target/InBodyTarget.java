package io.arconia.docling.client.convert.request.target;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Target for including the converted document directly in the body of the response as text.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InBodyTarget(@JsonProperty("kind") Target.Kind kind) implements Target {

    public InBodyTarget {
        kind = Target.Kind.INBODY;
    }

    public static InBodyTarget create() {
        return new InBodyTarget(Target.Kind.INBODY);
    }

}
