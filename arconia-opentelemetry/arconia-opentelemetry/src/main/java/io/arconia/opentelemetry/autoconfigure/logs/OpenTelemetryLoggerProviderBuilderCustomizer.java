package io.arconia.opentelemetry.autoconfigure.logs;

import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;

/**
 * Customizes the {@link SdkLoggerProviderBuilder} used to build the autoconfigured {@link SdkLoggerProvider}.
 */
@FunctionalInterface
public interface OpenTelemetryLoggerProviderBuilderCustomizer {

	void customize(SdkLoggerProviderBuilder builder);

}
