package io.arconia.dev.services.core.util;

import io.arconia.core.support.Internal;

/**
 * Utility class for container-related operations.
 */
@Internal
public final class ContainerUtils {

    /**
     * Whether the given port is valid.
     */
    public static boolean isValidPort(int port) {
        return port > 0 && port <= 65535;
    }

}
