package io.arconia.opentelemetry.autoconfigure.metrics;

import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;

/**
 * Customizes the {@link SdkMeterProviderBuilder} used to build the autoconfigured {@link SdkMeterProvider}.
 */
@FunctionalInterface
public interface OpenTelemetryMeterProviderBuilderCustomizer {

	void customize(SdkMeterProviderBuilder builder);

}
