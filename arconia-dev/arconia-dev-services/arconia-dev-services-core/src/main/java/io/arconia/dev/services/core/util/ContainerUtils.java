package io.arconia.dev.services.core.util;

import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;

/**
 * Utility class for container-related operations.
 */
@Incubating
public final class ContainerUtils {

    private ContainerUtils() {}

    /**
     * Whether the given port is a fixed port to expose to the host.
     * Returns {@code false} when the port is 0, meaning a random available port
     * will be assigned dynamically. Fails for out-of-range values, so that explicit
     * configuration errors are not silently ignored.
     */
    public static boolean isFixedPort(int port) {
        if (port == 0) {
            return false;
        }
        Assert.isTrue(port > 0 && port <= 65535,
                () -> "port must be between 0 and 65535, but was: " + port);
        return true;
    }

}
