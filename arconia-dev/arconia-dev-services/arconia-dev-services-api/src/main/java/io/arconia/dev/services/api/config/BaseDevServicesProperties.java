package io.arconia.dev.services.api.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.arconia.core.support.Incubating;

/**
 * Base properties for dev services.
 */
@Incubating
public interface BaseDevServicesProperties {

    /**
     * Whether the dev service is enabled.
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Full name of the container image used in the dev service.
     */
    String getImageName();

    /**
     * Environment variables to set in the service.
     */
    default Map<String,String> getEnvironment() {
        return Map.of();
    }

    /**
     * Network aliases to assign to the dev service container.
     */
    default List<String> getNetworkAliases() {
        return List.of();
    }

    /**
     * Fixed port for exposing the container's main service to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    default int getPort() {
        return 0;
    }

    /**
     * Resources from the classpath or host filesystem to copy into the container.
     * They can be files or directories that will be copied to the specified
     * destination path inside the container at startup and are immutable (read-only).
     */
    default List<ResourceMapping> getResources() {
        return List.of();
    }

    /**
     * Whether the dev service is shared among applications.
     * Only applicable in dev mode.
     */
    default boolean isShared() {
        return false;
    }

    /**
     * Maximum waiting time for the service to start.
     */
    default Duration getStartupTimeout() {
        return Duration.ofSeconds(30);
    }

    /**
     * Files or directories to mount from the host filesystem into the container.
     * They are mounted at the specified destination path inside the container
     * at startup and are mutable (read-write). Changes in either the host
     * or the container will be immediately reflected in the other.
     */
    default List<VolumeMapping> getVolumes() {
        return List.of();
    }

    // Setters

    default void setEnabled(boolean enabled) {}

    void setImageName(String imageName);

    default void setEnvironment(Map<String, String> environment) {}

    default void setNetworkAliases(List<String> networkAliases) {}

    default void setPort(int port) {}

    default void setResources(List<ResourceMapping> resources) {}

    default void setShared(boolean shared) {}

    default void setStartupTimeout(Duration startupTimeout) {}

    default void setVolumes(List<VolumeMapping> volumes) {}

}
