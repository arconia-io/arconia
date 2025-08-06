package io.arconia.docling.client.convert.request;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Target for sending the converted document to a specified URI.
 */
@Incubating(since = "0.15.0")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PutTarget(Kind kind, URI uri) implements Target {

    public PutTarget {
        Assert.notNull(uri, "URI cannot be null");
        kind = Kind.PUT;
    }

}
