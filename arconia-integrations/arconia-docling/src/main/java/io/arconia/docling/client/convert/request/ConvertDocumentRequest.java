package io.arconia.docling.client.convert.request;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

@Incubating(since = "0.15.0")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ConvertDocumentRequest(

        @JsonProperty("sources")
        List<? extends Source> sources,

        @JsonProperty("options")
        ConvertDocumentOptions options,
        
        @JsonProperty("target")
        @Nullable
        Target target

) {

    public ConvertDocumentRequest {
        Assert.notEmpty(sources, "sources cannot be null or empty");
        Assert.notNull(options, "options cannot be null");
        Assert.isTrue(sources.stream().allMatch(source -> source instanceof HttpSource)
                        || sources.stream().allMatch(source -> source instanceof FileSource),
                "All sources must be of the same type (HttpSource or FileSource)");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<Source> sources = new ArrayList<>();
        private ConvertDocumentOptions options = ConvertDocumentOptions.builder().build();
        @Nullable Target target;

        private Builder() {}

        public Builder httpSources(List<HttpSource> httpSources) {
            Assert.notNull(httpSources, "httpSources cannot be null");
            this.sources = new ArrayList<>(httpSources);
            return this;
        }

        public Builder addHttpSources(String... urls) {
            Assert.notNull(urls, "urls cannot be null");
            for (String url : urls) {
                Assert.notNull(url, "url cannot be null");
                this.sources.add(HttpSource.from(url));
            }
            return this;
        }

        public Builder addHttpSources(URI... urls) {
            Assert.notNull(urls, "urls cannot be null");
            for (URI url : urls) {
                Assert.notNull(url, "url cannot be null");
                this.sources.add(HttpSource.from(url));
            }
            return this;
        }

        public Builder fileSources(List<FileSource> fileSources) {
            Assert.notNull(fileSources, "fileSources cannot be null");
            this.sources = new ArrayList<>(fileSources);
            return this;
        }

        public Builder addFileSources(String filename, String base64String) {
            Assert.hasText(filename, "filename cannot be null or empty");
            Assert.hasText(base64String, "base64String cannot be null or empty");
            this.sources.add(FileSource.from(filename, base64String));
            return this;
        }

        public Builder options(ConvertDocumentOptions options) {
            this.options = options;
            return this;
        }

        public Builder target(@Nullable Target target) {
            this.target = target;
            return this;
        }

        public ConvertDocumentRequest build() {
            return new ConvertDocumentRequest(sources, options, target);
        }

    }

}
