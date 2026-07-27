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
     * The links exposed by this dev service. Resolved after the container has started,
     * so that mapped ports are available.
     */
    List<DevServiceLink> devServiceLinks();

}
