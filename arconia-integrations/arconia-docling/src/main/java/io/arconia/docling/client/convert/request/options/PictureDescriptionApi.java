package io.arconia.docling.client.convert.request.options;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

@Incubating(since = "0.15.0")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PictureDescriptionApi(

        @JsonProperty("url")
        URI url,

        @JsonProperty("headers")
        @Nullable
        Map<String,Object> headers,

        @JsonProperty("params")
        @Nullable
        Map<String,Object> params,

        @JsonProperty("timeout")
        @Nullable
        Duration timeout,

        @JsonProperty("prompt")
        @Nullable
        String prompt,

        @JsonProperty("concurrency")
        @Nullable
        Integer concurrency

) {

    public PictureDescriptionApi {
        Assert.notNull(url, "url cannot be null");
        if (headers != null) {
            headers = new HashMap<>(headers);
        }
        if (params != null) {
            params = new HashMap<>(params);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        @Nullable private URI url;
        @Nullable private Map<String,Object> headers;
        @Nullable private Map<String,Object> params;
        @Nullable private Duration timeout;
        @Nullable private String prompt;
        @Nullable private Integer concurrency;

        private Builder() {}

        /**
         * Endpoint which accepts OpenAI API compatible requests.
         */
        public Builder url(URI url) {
            this.url = url;
            return this;
        }

        /**
         * Headers used for calling the API endpoint.
         * For example, it could include authentication headers.
         */
        public Builder headers(Map<String,Object> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Model parameters.
         */
        public Builder params(Map<String,Object> params) {
            this.params = params;
            return this;
        }

        /**
         * Timeout for the API request.
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * Prompt used when calling the vision-language model.
         */
        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        /**
         * Maximum number of concurrent requests to the API.
         */
        public Builder concurrency(Integer concurrency) {
            this.concurrency = concurrency;
            return this;
        }

        public PictureDescriptionApi build() {
            return new PictureDescriptionApi(url, headers, params, timeout, prompt, concurrency);
        }

    }

}
