package io.arconia.opentelemetry.autoconfigure.sdk.metrics;

import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;

/**
 * Callback that can be used to customize the {@link SdkMeterProviderBuilder}
 * used to build the auto-configured {@link SdkMeterProvider}.
 */
@FunctionalInterface
public interface OpenTelemetryMeterProviderBuilderCustomizer {

	void customize(SdkMeterProviderBuilder builder);

}
