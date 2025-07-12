package io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.springframework.boot.info.OsInfo;
import org.springframework.util.StringUtils;

import io.arconia.core.info.HostInfo;
import io.arconia.core.support.Internal;

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
@Internal
public class HostResourceContributor implements ResourceContributor {

    // These semantic conventions are experimental, so we define them explicitly to be able to ensure backward
    // compatibility rather than using the constants from OpenTelemetry SemConv project that may change in the future
    // without considering backward compatibility.
    public static final AttributeKey<String> HOST_ARCH = AttributeKey.stringKey("host.arch");
    public static final AttributeKey<String> HOST_NAME = AttributeKey.stringKey("host.name");

    private final HostInfo hostInfo = new HostInfo();
    private final OsInfo osInfo = new OsInfo();

    @Override
    public void contribute(ResourceBuilder builder) {
        if (StringUtils.hasText(osInfo.getArch())) {
            builder.put(HOST_ARCH, osInfo.getArch());
        }
        if (StringUtils.hasText(hostInfo.getName())) {
            builder.put(HOST_NAME, hostInfo.getName());
        }
    }

}
