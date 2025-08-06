package io.arconia.docling.client.convert.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.arconia.core.support.Incubating;

/**
 * Target for including the converted document directly in the body of the response as text.
 */
@Incubating(since = "0.15.0")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InBodyTarget(Target.Kind kind) implements Target {

    public InBodyTarget {
        kind = Target.Kind.INBODY;
    }
    
    public static InBodyTarget create() {
        return new InBodyTarget(Target.Kind.INBODY);
    }

}
