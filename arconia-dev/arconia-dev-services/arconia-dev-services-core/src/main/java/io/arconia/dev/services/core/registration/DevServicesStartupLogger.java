package io.arconia.dev.services.core.registration;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.arconia.dev.services.api.registration.DevServiceLink;

/**
 * Logs consistent startup messages for each dev service.
 */
final class DevServicesStartupLogger {

    private static final Logger logger = LoggerFactory.getLogger("io.arconia.dev.services.startup");

    private DevServicesStartupLogger() {}

    /**
     * Log a startup message for an owned dev service.
     */
    static void owned(String name, @Nullable String containerId, DevServicesRegistry.@Nullable ReuseDecision reuse, List<DevServiceLink> links) {
        logger.info("Dev Service '{}' is ready{}{}{}",
                name, formatContainer(containerId), formatReuse(reuse), formatLinks(links));
    }

    /**
     * Log a startup message for a discovered dev service.
     */
    static void discovered(String name, String containerId, List<DevServiceLink> links) {
        logger.info("Dev Service '{}' is ready{} started by another application (reuse-strategy: framework){}",
                name, formatContainer(containerId), formatLinks(links));
    }

    /**
     * The container id in the short form used by the container runtime CLIs.
     */
    static String computeContainerShortId(@Nullable String containerId) {
        if (containerId == null) {
            return "<none>";
        }
        return (containerId.length() > 12) ? containerId.substring(0, 12) : containerId;
    }

    /**
     * Render the container details the dev service runs in. A container that has not been started has no id yet.
     */
    private static String formatContainer(@Nullable String containerId) {
        return (containerId != null) ? " in container " + computeContainerShortId(containerId) : "";
    }

    /**
     * Render the reuse strategy in effect and, when the configured strategy could not be
     * applied, the reason why.
     */
    private static String formatReuse(DevServicesRegistry.@Nullable ReuseDecision reuse) {
        if (reuse == null) {
            return "";
        }
        String effective = reuse.effective().name().toLowerCase(Locale.ROOT);
        return " (reuse-strategy: " + ((reuse.reason() != null) ? effective + ", " + reuse.reason() : effective) + ")";
    }

    /**
     * Render the links defined for the dev service.
     */
    private static String formatLinks(List<DevServiceLink> links) {
        return links.isEmpty() ? "" : links.stream()
                .map(link -> link.label() + ": " + link.url())
                .collect(Collectors.joining(", ", " - ", ""));
    }

}
