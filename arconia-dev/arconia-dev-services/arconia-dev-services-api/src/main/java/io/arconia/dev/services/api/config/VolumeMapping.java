package io.arconia.dev.services.api.config;

/**
 * Mapping of paths to be mounted from the host filesystem into a container.
 */
public final class VolumeMapping {

    /**
     * Path to the file/directory on the host filesystem.
     */
    public String hostPath;

    /**
     * Path to the file/directory inside the container.
     */
    public String containerPath;

    public VolumeMapping(String hostPath, String containerPath) {
        this.hostPath = hostPath;
        this.containerPath = containerPath;
    }

    public String getHostPath() {
        return hostPath;
    }

    public void setHostPath(String hostPath) {
        this.hostPath = hostPath;
    }

    public String getContainerPath() {
        return containerPath;
    }

    public void setContainerPath(String containerPath) {
        this.containerPath = containerPath;
    }

}
