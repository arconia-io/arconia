package io.arconia.opentelemetry.autoconfigure.sdk.metrics;

import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;

/**
 * Callback that can be used to customize the {@link SdkMeterProviderBuilder}
 * used to build the auto-configured {@link SdkMeterProvider}.
 *
 * @deprecated in favour of {@link OpenTelemetryMeterProviderBuilderCustomizer}.
 */
@FunctionalInterface
@Deprecated(since = "0.12.0", forRemoval = true)
public interface SdkMeterProviderBuilderCustomizer {

	void customize(SdkMeterProviderBuilder builder);

}
