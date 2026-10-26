package io.arconia.opentelemetry.autoconfigure.resource.contributor;

import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.TelemetryAttributes;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link DistroResourceContributor}.
 */
class DistroResourceContributorTests {

    private final ResourceBuilder resourceBuilder = mock(ResourceBuilder.class);

    @Test
    void shouldAddDistroNameAndVersion() {
        DistroResourceContributor contributor = new DistroResourceContributor("1.2.3");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(TelemetryAttributes.TELEMETRY_DISTRO_NAME, "arconia-opentelemetry");
        verify(resourceBuilder).put(TelemetryAttributes.TELEMETRY_DISTRO_VERSION, "1.2.3");
    }

    @Test
    void shouldAddOnlyDistroNameWhenVersionUnknown() {
        DistroResourceContributor contributor = new DistroResourceContributor(null);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(TelemetryAttributes.TELEMETRY_DISTRO_NAME, "arconia-opentelemetry");
        verify(resourceBuilder, never()).put(eq(TelemetryAttributes.TELEMETRY_DISTRO_VERSION), any(String.class));
    }

    @Test
    void shouldAddOnlyDistroNameWhenVersionBlank() {
        DistroResourceContributor contributor = new DistroResourceContributor("  ");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(TelemetryAttributes.TELEMETRY_DISTRO_NAME, "arconia-opentelemetry");
        verify(resourceBuilder, never()).put(eq(TelemetryAttributes.TELEMETRY_DISTRO_VERSION), any(String.class));
    }

}
