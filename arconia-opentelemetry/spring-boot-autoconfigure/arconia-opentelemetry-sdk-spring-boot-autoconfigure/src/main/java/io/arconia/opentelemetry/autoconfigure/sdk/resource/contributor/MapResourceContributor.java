package io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor;

import java.util.Map;

import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.springframework.util.Assert;

/**
 * A {@link ResourceContributor} that contributes attributes from a {@link Map}.
 */
public class MapResourceContributor implements ResourceContributor {

    private final Map<String, String> attributes;

    public MapResourceContributor(Map<String, String> attributes) {
        Assert.notNull(attributes, "attributes cannot be null");
        Assert.noNullElements(attributes.keySet(), "attributes keys cannot be null");
        Assert.noNullElements(attributes.values(), "attributes values cannot be null");
        this.attributes = attributes;
    }

    @Override
    public void contribute(ResourceBuilder builder) {
        attributes.forEach(builder::put);
    }

}
