package io.arconia.core.info;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Information about the host the application is running on.
 */
public final class HostInfo {

    private static final Logger logger = LoggerFactory.getLogger(HostInfo.class);

    @Nullable
    private final String arch;
    @Nullable
    private final String name;

    public HostInfo() {
        this.arch = System.getProperty("os.arch");
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            logger.debug("Failed to get hostname", ex);
        }
        this.name = hostName;
    }

    @Nullable
    public String getArch() {
        return arch;
    }

    @Nullable
    public String getName() {
        return name;
    }

}
