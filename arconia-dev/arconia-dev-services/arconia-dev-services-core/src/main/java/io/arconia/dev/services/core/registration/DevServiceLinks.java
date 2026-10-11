package io.arconia.dev.services.core.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration.Origin;
import io.arconia.dev.services.core.registration.DevServiceDefinition.LinkDefinition;

/**
 * Resolves the links a dev service declares, in terms of container ports,
 * against the port mapping of the container the dev service runs in.
 */
final class DevServiceLinks {

    private static final Logger logger = LoggerFactory.getLogger(DevServiceLinks.class);

    private DevServiceLinks() {}

    /**
     * Resolve the links of the given dev service against the given host and port mapping.
     * A link whose port cannot be resolved is skipped, so that the others are still reported.
     * For a managed container, an unresolved port points at a container that does not expose it
     * and is reported as a warning; a discovered container is not under the application's control.
     */
    static List<DevServiceLink> resolve(DevServiceDefinition service, String host, IntUnaryOperator portMapper, Origin origin) {
        List<DevServiceLink> links = new ArrayList<>();
        for (LinkDefinition link : service.links()) {
            try {
                links.add(link.resolve(host, portMapper.applyAsInt(link.port())));
            } catch (Exception ex) {
                if (origin == Origin.MANAGED) {
                    logger.warn("Dev Service '{}': skipping the '{}' link, port {} could not be resolved. Make sure the container exposes it.",
                            service.name(), link.label(), link.port(), ex);
                } else {
                    logger.debug("Dev Service '{}': skipping the '{}' link, port {} is not published by the discovered container",
                            service.name(), link.label(), link.port(), ex);
                }
            }
        }
        return List.copyOf(links);
    }

}
