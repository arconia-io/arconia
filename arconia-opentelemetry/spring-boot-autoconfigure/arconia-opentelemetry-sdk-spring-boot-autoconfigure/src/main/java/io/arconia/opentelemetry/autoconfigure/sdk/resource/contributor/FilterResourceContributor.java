package io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor;

import java.util.List;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.springframework.util.Assert;

/**
 * A {@link ResourceContributor} that filters out keys from a {@link Resource}.
 */
public class FilterResourceContributor implements ResourceContributor {

    private final List<String> disabledKeys;

    public FilterResourceContributor(List<String> disabledKeys) {
        Assert.notNull(disabledKeys, "disabledKeys cannot be null");
        this.disabledKeys = disabledKeys;
    }

    @Override
    public void contribute(ResourceBuilder builder) {
        builder.removeIf(key -> disabledKeys.contains(key.getKey()));
    }

}
