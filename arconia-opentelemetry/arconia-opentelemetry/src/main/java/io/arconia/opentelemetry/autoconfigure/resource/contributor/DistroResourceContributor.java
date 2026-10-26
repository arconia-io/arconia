package io.arconia.opentelemetry.autoconfigure.resource.contributor;

import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.TelemetryAttributes;

import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import io.arconia.core.support.Incubating;

/**
 * A {@link ResourceContributor} that contributes information about the Arconia OpenTelemetry distribution.
 * <p>
 * The following attributes are populated:
 * <ul>
 *     <li>{@code telemetry.distro.name}</li>
 *     <li>{@code telemetry.distro.version}</li>
 * </ul>
 *
 * @link <a href="https://opentelemetry.io/docs/specs/semconv/resource/#telemetry-distribution">Resource Telemetry Distribution Semantic Conventions</a>
 */
@Incubating
public final class DistroResourceContributor implements ResourceContributor {

    public static final String DISTRO_NAME = "arconia-opentelemetry";

    private final @Nullable String distroVersion;

    public DistroResourceContributor() {
        this(DistroResourceContributor.class.getPackage().getImplementationVersion());
    }

    DistroResourceContributor(@Nullable String distroVersion) {
        this.distroVersion = distroVersion;
    }

    @Override
    public void contribute(ResourceBuilder builder) {
        builder.put(TelemetryAttributes.TELEMETRY_DISTRO_NAME, DISTRO_NAME);
        if (StringUtils.hasText(distroVersion)) {
            builder.put(TelemetryAttributes.TELEMETRY_DISTRO_VERSION, distroVersion);
        }
    }

}
