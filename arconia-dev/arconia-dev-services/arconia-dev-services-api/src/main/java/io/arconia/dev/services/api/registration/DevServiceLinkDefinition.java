package io.arconia.dev.services.api.registration;

import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * The definition of a {@link DevServiceLink}, expressed in terms of the container port the
 * link points to rather than a resolved URL.
 */
@Incubating
public record DevServiceLinkDefinition(
        String id,
        String label,
        String scheme,
        int port,
        String path
) {

    /**
     * The scheme used when none is specified.
     */
    public static final String DEFAULT_SCHEME = "http";

    /**
     * The identifiers a link can be given. Kept to the character set Docker labels use by
     * convention, since the id becomes part of the label naming the link on the container.
     */
    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9]([a-z0-9-]*[a-z0-9])?");

    private static final Pattern SCHEME_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9+.-]*");

    public DevServiceLinkDefinition {
        Assert.hasText(id, "id cannot be null or empty");
        Assert.isTrue(ID_PATTERN.matcher(id).matches(),
                "id must contain only lowercase letters, digits, and dashes: %s".formatted(id));
        Assert.hasText(label, "label cannot be null or empty");
        Assert.isTrue(label.chars().noneMatch(Character::isISOControl),
                "label cannot contain control characters: %s".formatted(label));
        Assert.hasText(scheme, "scheme cannot be null or empty");
        Assert.isTrue(SCHEME_PATTERN.matcher(scheme).matches(), "scheme is not a valid URI scheme: %s".formatted(scheme));
        Assert.isTrue(port > 0 && port <= 65535, "port must be between 1 and 65535");
        Assert.notNull(path, "path cannot be null");
        Assert.isTrue(path.isEmpty() || path.startsWith("/"), "path must be empty or start with '/': %s".formatted(path));
        Assert.isTrue(path.chars().noneMatch(Character::isISOControl),
                "path cannot contain control characters: %s".formatted(path));
    }

    /**
     * Resolve this definition into a link reachable at the given host and mapped port.
     */
    public DevServiceLink toLink(String host, int mappedPort) {
        Assert.hasText(host, "host cannot be null or empty");
        return DevServiceLink.builder()
                .id(id)
                .label(label)
                .url("%s://%s:%d%s".formatted(scheme, host, mappedPort, path))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private @Nullable String id;

        private @Nullable String label;

        private String scheme = DEFAULT_SCHEME;

        private int port;

        private String path = "";

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
         * The scheme of the link, {@code http} by default.
         */
        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        /**
         * The container port the link points to, before any port mapping is applied.
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * The path of the link (e.g. {@code /console}), empty by default.
         */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public DevServiceLinkDefinition build() {
            return new DevServiceLinkDefinition(id, label, scheme, port, path);
        }

    }

}
