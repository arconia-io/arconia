package io.arconia.docling.client.convert.request;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Represents an HTTP source for a document to convert.
 */
@Incubating(since = "0.15.0")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HttpSource(

        @JsonProperty("kind")
        Source.Kind kind,

        @JsonProperty("url")
        URI url,
        
        @JsonProperty("headers")
        @Nullable
        Map<String, Object> headers

) implements Source {

    public HttpSource {
        Assert.notNull(url, "url cannot be null");
        kind = Source.Kind.HTTP;
    }

    public static HttpSource from(String url) {
        return HttpSource.builder().url(URI.create(url)).build();
    }

    public static HttpSource from(URI url) {
        return HttpSource.builder().url(url).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        @Nullable private URI url;
        @Nullable private Map<String, Object> headers;

        private Builder() {}

        /**
         * HTTP url to process.
         */
        public Builder url(URI url) {
            this.url = url;
            return this;
        }

        /**
         * Additional headers used to fetch the urls (e.g. authorization or agent).
         */
        public Builder headers(Map<String, Object> headers) {
            this.headers = new HashMap<>(headers);
            return this;
        }

        public HttpSource build() {
            return new HttpSource(Source.Kind.HTTP, url, headers);
        }

    }

}
