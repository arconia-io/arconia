package io.arconia.dev.services.core.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration.Origin;

/**
 * Logs startup messages for each dev service once its container is ready.
 */
final class DevServicesStartupLogger {

    private static final Logger logger = LoggerFactory.getLogger("io.arconia.dev.services.startup");

    private DevServicesStartupLogger() {}

    /**
     * Log that the dev service is ready in the given container
     */
    static void ready(String name, @Nullable String containerId, String imageName, Origin origin, boolean kept,
            DevServicesRegistry.@Nullable ReuseDecision reuse, List<DevServiceLink> links) {
        List<String> facts = new ArrayList<>();
        facts.add(imageName);
        if (origin == Origin.DISCOVERED) {
            facts.add("started by another application");
        }
        if (kept) {
            facts.add("kept across restart");
        }
        if (reuse != null) {
            facts.add(formatReuse(reuse));
        }
        logger.info("Dev Service '{}' is ready in container {} ({})", name, ContainerRuntimeInfo.shortId(containerId), String.join(", ", facts));
        if (!links.isEmpty()) {
            logger.info("Dev Service '{}' links: {}", name, formatLinks(links));
        }
    }

    /**
     * Render the reuse strategy in effect and, when the configured strategy could not be
     * applied, the reason why.
     */
    private static String formatReuse(DevServicesRegistry.ReuseDecision reuse) {
        String effective = "reuse-strategy: " + reuse.effective().name().toLowerCase(Locale.ROOT);
        return (reuse.reason() != null) ? effective + " (" + reuse.reason() + ")" : effective;
    }

    /**
     * Render the links the dev service exposes.
     */
    private static String formatLinks(List<DevServiceLink> links) {
        return links.stream()
                .map(link -> link.label() + ": " + link.url())
                .collect(Collectors.joining(", "));
    }

}
