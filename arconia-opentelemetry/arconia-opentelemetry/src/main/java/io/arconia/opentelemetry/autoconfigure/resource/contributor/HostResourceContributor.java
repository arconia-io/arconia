package io.arconia.opentelemetry.autoconfigure.resource.contributor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Supplier;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.info.OsInfo;
import org.springframework.util.StringUtils;

import io.arconia.core.support.Incubating;

/**
 * A {@link ResourceContributor} that contributes attributes about the host the application is running on,
 * following the OpenTelemetry Semantic Conventions.
 * <p>
 * The following attributes are populated:
 * <ul>
 *     <li>{@code host.arch}</li>
 *     <li>{@code host.name}</li>
 * </ul>
 *
 * @link <a href="https://opentelemetry.io/docs/specs/semconv/resource/host/">Resource Host Semantic Conventions</a>
 */
@Incubating
public final class HostResourceContributor implements ResourceContributor {

    private static final Logger logger = LoggerFactory.getLogger(HostResourceContributor.class);
    private static final Supplier<@Nullable String> DEFAULT_HOST_NAME_SUPPLIER = HostResourceContributor::getHostName;

    // These semantic conventions are experimental, so we define them explicitly to be able to ensure backward
    // compatibility rather than using the constants from OpenTelemetry SemConv project that may change in the future
    // without considering backward compatibility.
    public static final AttributeKey<String> HOST_ARCH = AttributeKey.stringKey("host.arch");
    public static final AttributeKey<String> HOST_NAME = AttributeKey.stringKey("host.name");

    private final Supplier<@Nullable String> hostNameSupplier;
    private final OsInfo osInfo = new OsInfo();

    public HostResourceContributor() {
        this(DEFAULT_HOST_NAME_SUPPLIER);
    }

    public HostResourceContributor(Supplier<@Nullable String> hostNameSupplier) {
        this.hostNameSupplier = hostNameSupplier;
    }

    @Override
    public void contribute(ResourceBuilder builder) {
        if (StringUtils.hasText(osInfo.getArch())) {
            builder.put(HOST_ARCH, osInfo.getArch());
        }
        String hostName = hostNameSupplier.get();
        if (StringUtils.hasText(hostName)) {
            builder.put(HOST_NAME, hostName);
        }
    }

    @Nullable
    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            logger.debug("Failed to get hostname", ex);
            return null;
        }
    }

}
