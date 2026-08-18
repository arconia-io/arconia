package io.arconia.dev.services.core.registration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;

import io.arconia.dev.services.api.registration.DevServiceLabels;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceLinkDefinition;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;

/**
 * The links a dev service exposes, from the definitions its container declares to the
 * URLs reported in startup logs and developer tooling.
 * <p>
 * A dev service declares its links once, in terms of container ports. They are resolved
 * against the port mappings of a container this application started, or of one adopted from
 * another application, in which case the definitions are read back from the container labels
 * the owning application wrote.
 */
final class DevServiceLinks {

    private static final Logger logger = LoggerFactory.getLogger(DevServiceLinks.class);

    private DevServiceLinks() {}

    /**
     * The link definitions declared by the given container, if any.
     * <p>
     * Definitions are read before the container starts, so that they can be recorded as
     * container labels. A dev service that fails to declare them, or that declares two links
     * with the same id, is reported and its offending links skipped.
     */
    static List<DevServiceLinkDefinition> declaredBy(Container<?> container) {
        if (!(container instanceof DevServiceLinkProvider linkProvider)) {
            return List.of();
        }
        try {
            List<DevServiceLinkDefinition> definitions = linkProvider.devServiceLinkDefinitions();
            if (definitions == null) {
                return List.of();
            }
            Map<String, DevServiceLinkDefinition> byId = new LinkedHashMap<>();
            for (DevServiceLinkDefinition definition : definitions) {
                if (definition == null) {
                    continue;
                }
                DevServiceLinkDefinition previous = byId.putIfAbsent(definition.id(), definition);
                if (previous != null) {
                    // A container label can hold only one link per id, so a duplicate would make an
                    // adopting application report fewer links than the one that started the container.
                    logger.warn("Ignoring a duplicate '{}' link declared by a dev service container", definition.id());
                }
            }
            return List.copyOf(byId.values());
        } catch (Exception ex) {
            logger.warn("Failed to read the link definitions of a dev service container", ex);
            return List.of();
        }
    }

    /**
     * The links exposed by a container this application started, resolved from the
     * definitions the container declares and its port mappings.
     * <p>
     * A definition that cannot be resolved is reported at warning level: the container
     * declares a link to a port it doesn't expose, which the dev service can fix.
     */
    static List<DevServiceLink> resolve(Container<?> container) {
        List<DevServiceLinkDefinition> definitions = declaredBy(container);
        if (definitions.isEmpty()) {
            return List.of();
        }
        return resolve(definitions, container.getHost(), container::getMappedPort, true);
    }

    /**
     * The links exposed by a discovered dev service, resolved from the definitions its
     * container carries as labels and the port mappings the container runtime reports.
     * <p>
     * The definitions come from the application that started the container, so the links
     * describe it as it actually runs rather than as this application would have configured it.
     * A definition that cannot be resolved is reported at debug level only: the port it points
     * to may simply not have been exposed by that application, which is not this one's business.
     */
    static List<DevServiceLink> resolve(DiscoveredContainer discoveredContainer) {
        return resolve(DevServiceLabels.linksFrom(discoveredContainer.containerInfo().labels()),
                discoveredContainer.host(), discoveredContainer::mappedPort, false);
    }

    /**
     * Resolve each link definition against the given host and port mapping, skipping the ones
     * that cannot be resolved rather than losing the others.
     */
    private static List<DevServiceLink> resolve(List<DevServiceLinkDefinition> definitions, String host,
            IntUnaryOperator portMapper, boolean owned) {
        List<DevServiceLink> links = new ArrayList<>();
        for (DevServiceLinkDefinition definition : definitions) {
            try {
                links.add(definition.toLink(host, portMapper.applyAsInt(definition.port())));
            } catch (Exception ex) {
                if (owned) {
                    logger.warn("Skipping the '{}' link of a dev service container: port {} could not be resolved. "
                            + "Make sure the container exposes it.", definition.id(), definition.port(), ex);
                } else {
                    logger.debug("Skipping the '{}' link of a discovered dev service: port {} is not mapped",
                            definition.id(), definition.port(), ex);
                }
            }
        }
        return List.copyOf(links);
    }

}
