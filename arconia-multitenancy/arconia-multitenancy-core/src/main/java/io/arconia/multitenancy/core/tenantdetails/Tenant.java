package io.arconia.multitenancy.core.tenantdetails;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Default implementation to hold tenant details.
 */
@Incubating(since = "0.3.0")
public class Tenant implements TenantDetails {

    private final String identifier;

    private final boolean enabled;

    private final Map<String, Object> attributes;

    protected Tenant(String identifier, boolean enabled, @Nullable Map<String, Object> attributes) {
        Assert.hasText(identifier, "identifier cannot be null or empty");

        this.identifier = identifier;
        this.enabled = enabled;
        this.attributes = Objects.requireNonNullElse(attributes, new HashMap<>());
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {

        private String identifier;

        private boolean enabled = true;

        private Map<String, Object> attributes = new HashMap<>();

        public Builder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder addAttribute(String key, Object value) {
            attributes.put(key, value);
            return this;
        }

        public Tenant build() {
            return new Tenant(identifier, enabled, attributes);
        }

    }

}
