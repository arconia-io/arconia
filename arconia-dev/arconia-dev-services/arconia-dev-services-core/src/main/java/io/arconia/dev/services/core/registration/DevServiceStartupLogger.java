package io.arconia.dev.services.core.registration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.api.registration.DevServiceRegistration.Origin;

/**
 * Logs startup messages for each dev service once its container is ready.
 */
final class DevServiceStartupLogger {

    private static final Logger logger = LoggerFactory.getLogger("io.arconia.dev.services.startup");

    private DevServiceStartupLogger() {}

    /**
     * Log that the registered dev service is ready in the given container, with the image it
     * runs, whether it was kept across a DevTools restart, the reuse strategy in effect
     * when it applies, and whether it uses SSL.
     */
    static void ready(DevServiceRegistration registration, @Nullable String containerId, @Nullable String imageName,
            boolean kept, @Nullable ReuseDecision reuse, boolean ssl) {
        List<String> facts = new ArrayList<>();
        if (imageName != null) {
            facts.add(imageName);
        }
        if (registration.getOrigin() == Origin.DISCOVERED) {
            facts.add("started by another application");
        }
        if (kept) {
            facts.add("kept across restart");
        }
        if (reuse != null) {
            facts.add(formatReuse(reuse));
        }
        if (ssl) {
            facts.add("ssl");
        }
        String details = facts.isEmpty() ? "" : " (" + String.join(", ", facts) + ")";
        logger.info("Dev Service '{}' is ready in container {}{}", registration.getName(), ContainerRuntimeInfo.shortId(containerId), details);
        if (!registration.getLinks().isEmpty()) {
            logger.info("Dev Service '{}' links: {}", registration.getName(), formatLinks(registration.getLinks()));
        }
    }

    /**
     * Render the reuse strategy in effect and, when the configured strategy could not be
     * applied, the reason why.
     */
    private static String formatReuse(ReuseDecision reuse) {
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
