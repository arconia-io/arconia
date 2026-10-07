package io.arconia.dev.services.api.config;

import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Mapping of a file or directory to be mounted from the host filesystem into a container.
 *
 * @param hostPath path to the file or directory on the host filesystem
 * @param containerPath path to the file or directory inside the container
 */
@Incubating
public record VolumeMapping(String hostPath, String containerPath) {

    public VolumeMapping {
        Assert.hasText(hostPath, "hostPath cannot be null or empty");
        Assert.hasText(containerPath, "containerPath cannot be null or empty");
    }

}
