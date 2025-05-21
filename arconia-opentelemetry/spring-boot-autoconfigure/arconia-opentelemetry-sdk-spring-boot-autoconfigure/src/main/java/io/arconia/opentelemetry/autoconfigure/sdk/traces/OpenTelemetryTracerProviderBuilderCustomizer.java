package io.arconia.opentelemetry.autoconfigure.sdk.traces;

import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;

/**
 * Callback for customizing the {@link SdkTracerProviderBuilder}
 * used to build the auto-configured {@link SdkTracerProvider}.
 */
@FunctionalInterface
public interface OpenTelemetryTracerProviderBuilderCustomizer {

	void customize(SdkTracerProviderBuilder builder);

}
