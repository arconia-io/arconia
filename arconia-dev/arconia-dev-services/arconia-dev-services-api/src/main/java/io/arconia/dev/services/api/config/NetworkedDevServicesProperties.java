package io.arconia.dev.services.api.config;

import io.arconia.core.support.Incubating;

/**
 * Properties for dev services that can join a shared OCI network,
 * so their containers can communicate with other dev service containers.
 */
@Incubating
public interface NetworkedDevServicesProperties extends BaseDevServicesProperties {

    /**
     * Whether the dev service joins the shared dev services network, so its container
     * can communicate with other dev service containers over an OCI network
     * (for example, to send telemetry to an observability dev service).
     * <p>
     * This is distinct from {@code shared}, which shares the same container across
     * applications; joining a network only affects container-to-container
     * connectivity within the network. Only applicable in dev and test mode.
     */
    default boolean isJoinNetwork() {
        return false;
    }

}
