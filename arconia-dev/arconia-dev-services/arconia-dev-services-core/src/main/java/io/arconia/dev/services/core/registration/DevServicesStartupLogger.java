package io.arconia.dev.services.core.registration;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.arconia.dev.services.api.registration.DevServiceLink;

/**
 * Logs a consistent, Arconia-controlled startup message for each dev service, replacing the ad-hoc
 * messages emitted by the underlying container classes.
 */
final class DevServicesStartupLogger {

    private static final Logger logger = LoggerFactory.getLogger("io.arconia.dev.services.startup");

    private DevServicesStartupLogger() {}

    static void owned(String name, List<DevServiceLink> links) {
        logger.info("Dev Service '{}' is ready{}", name, formatLinks(links));
    }

    static void discovered(String name, List<DevServiceLink> links) {
        logger.info("Dev Service '{}' is shared from another application{}", name, formatLinks(links));
    }

    /**
     * Render the links inline on the same log record as the service, so each dev service produces a
     * single, compact log line rather than one record per link.
     */
    private static String formatLinks(List<DevServiceLink> links) {
        return links.isEmpty() ? "" : links.stream()
                .map(link -> link.label() + ": " + link.url())
                .collect(Collectors.joining(", ", " - ", ""));
    }

}
