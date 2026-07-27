package io.arconia.dev.services.api.registration;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * A named link exposed by a dev service, such as a management console or a telemetry endpoint,
 * shown in startup logs and developer tooling.
 */
@Incubating
public record DevServiceLink(
        String id,
        String label,
        String url
) {

    public DevServiceLink {
        Assert.hasText(id, "id cannot be null or empty");
        Assert.hasText(label, "label cannot be null or empty");
        Assert.hasText(url, "url cannot be null or empty");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private @Nullable String id;

        private @Nullable String label;

        private @Nullable String url;

        private Builder() {}

        /**
         * A stable identifier for the kind of link (e.g. {@code grafana}, {@code otlp}).
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * The human-readable label of the link (e.g. {@code Grafana}, {@code OTLP/HTTP}).
         */
        public Builder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * The URL the link points to (e.g., a management console or telemetry endpoint).
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public DevServiceLink build() {
            return new DevServiceLink(id, label, url);
        }

    }

}
