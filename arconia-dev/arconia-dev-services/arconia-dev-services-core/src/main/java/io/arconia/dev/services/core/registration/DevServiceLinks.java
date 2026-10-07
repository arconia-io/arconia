package io.arconia.dev.services.core.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.core.registration.ServiceSpec.LinkDefinition;

/**
 * Resolves the links a dev service declares, in terms of container ports, into the URLs
 * reported in the startup message and in developer tooling, against the port mapping of the
 * container the dev service runs in.
 */
final class DevServiceLinks {

    private static final Logger logger = LoggerFactory.getLogger(DevServiceLinks.class);

    private DevServiceLinks() {}

    /**
     * Resolve the links of the given dev service against the given host and port mapping.
     * A link whose port cannot be resolved is skipped, so that the others are still reported:
     * with a warning for a container this application started, since the dev service declares
     * a port its container doesn't expose, and at debug level for a discovered container, which
     * the application that started it may not have published that port of.
     */
    static List<DevServiceLink> resolve(ServiceSpec service, String host, IntUnaryOperator portMapper, boolean owned) {
        List<DevServiceLink> links = new ArrayList<>();
        for (LinkDefinition link : service.getLinks()) {
            try {
                links.add(link.resolve(host, portMapper.applyAsInt(link.port())));
            } catch (Exception ex) {
                if (owned) {
                    logger.warn("Skipping the '{}' link of the '{}' dev service: port {} could not be resolved. Make sure the container exposes it.",
                            link.label(), service.getName(), link.port(), ex);
                } else {
                    logger.debug("Skipping the '{}' link of the '{}' dev service: port {} is not published by the discovered container",
                            link.label(), service.getName(), link.port(), ex);
                }
            }
        }
        return List.copyOf(links);
    }

}
