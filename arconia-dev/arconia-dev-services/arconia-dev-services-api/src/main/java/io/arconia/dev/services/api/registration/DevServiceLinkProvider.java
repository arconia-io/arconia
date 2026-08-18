package io.arconia.dev.services.api.registration;

import java.util.List;

import io.arconia.core.support.Incubating;

/**
 * Implemented by dev service containers that expose one or more {@link DevServiceLink links},
 * such as a management console or a telemetry endpoint, to be shown in startup logs and
 * developer tooling.
 */
@Incubating
public interface DevServiceLinkProvider {

    /**
     * The links exposed by this dev service, declared in terms of container ports.
     * <p>
     * The framework resolves each definition into a {@link DevServiceLink} once the port
     * mapping is known, and publishes the definitions as container labels so that an
     * application adopting this container as a shared dev service reports the same links
     * without declaring them again.
     * <p>
     * This method is called <em>before the container starts</em>, since the labels are applied
     * when the container is created, and again once it is running. It must therefore return the
     * same definitions both times and must not read state that only a started container has,
     * such as its mapped ports: capture whatever the links depend on when the container is
     * constructed. Declare only ports the container actually exposes, and give each link a
     * distinct id.
     */
    List<DevServiceLinkDefinition> devServiceLinkDefinitions();

}
