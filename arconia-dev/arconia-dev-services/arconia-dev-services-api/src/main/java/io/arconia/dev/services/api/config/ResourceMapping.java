package io.arconia.dev.services.api.config;

import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Mapping of a resource to be copied into a container at startup.
 *
 * @param sourcePath path to the resource in the classpath or host filesystem
 * @param containerPath path to the resource inside the container
 */
@Incubating
public record ResourceMapping(String sourcePath, String containerPath) {

    public ResourceMapping {
        Assert.hasText(sourcePath, "sourcePath cannot be null or empty");
        Assert.hasText(containerPath, "containerPath cannot be null or empty");
    }

}
