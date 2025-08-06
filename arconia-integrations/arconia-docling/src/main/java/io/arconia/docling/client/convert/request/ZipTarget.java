package io.arconia.docling.client.convert.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.arconia.core.support.Incubating;

/**
 * Target for zipping the converted document and including it in the response.
 */
@Incubating(since = "0.15.0")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ZipTarget(Target.Kind kind) implements Target {

    public ZipTarget {
        kind = Target.Kind.ZIP;
    }

    public static ZipTarget create() {
        return new ZipTarget(Target.Kind.ZIP);
    }

}
