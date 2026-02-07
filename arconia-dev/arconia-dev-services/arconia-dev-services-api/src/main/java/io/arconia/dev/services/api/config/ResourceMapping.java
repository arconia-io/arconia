package io.arconia.dev.services.api.config;

/**
 * Mapping of resources to be mounted into the container.
 */
public final class ResourceMapping {

    /**
     * Path to the resource in the classpath or host filesystem.
     */
    public String sourcePath;

    /**
     * Path to the resource inside the container.
     */
    public String containerPath;

    public ResourceMapping(String sourcePath, String containerPath) {
        this.sourcePath = sourcePath;
        this.containerPath = containerPath;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getContainerPath() {
        return containerPath;
    }

    public void setContainerPath(String containerPath) {
        this.containerPath = containerPath;
    }

}
