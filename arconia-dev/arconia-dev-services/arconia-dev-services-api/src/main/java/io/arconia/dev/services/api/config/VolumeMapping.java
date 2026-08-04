package io.arconia.dev.services.api.config;

import io.arconia.core.support.Incubating;

/**
 * Mapping of paths to be mounted from the host filesystem into a container.
 */
@Incubating
public final class VolumeMapping {

    /**
     * Path to the file/directory on the host filesystem.
     */
    private final String hostPath;

    /**
     * Path to the file/directory inside the container.
     */
    private final String containerPath;

    public VolumeMapping(String hostPath, String containerPath) {
        this.hostPath = hostPath;
        this.containerPath = containerPath;
    }

    public String getHostPath() {
        return hostPath;
    }

    public String getContainerPath() {
        return containerPath;
    }

}
