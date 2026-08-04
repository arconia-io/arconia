package io.arconia.dev.services.api.config;

import io.arconia.core.support.Incubating;

/**
 * Mapping of resources to be copied into a container.
 */
@Incubating
public final class ResourceMapping {

    /**
     * Path to the resource in the classpath or host filesystem.
     */
    private final String sourcePath;

    /**
     * Path to the resource inside the container.
     */
    private final String containerPath;

    public ResourceMapping(String sourcePath, String containerPath) {
        this.sourcePath = sourcePath;
        this.containerPath = containerPath;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getContainerPath() {
        return containerPath;
    }

}
