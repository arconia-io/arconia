package io.arconia.opentelemetry.autoconfigure.sdk.logs;

import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;

/**
 * Callback for customizing the {@link SdkLoggerProviderBuilder}
 * used to build the auto-configured {@link SdkLoggerProvider}.
 */
@FunctionalInterface
public interface OpenTelemetryLoggerProviderBuilderCustomizer {

	void customize(SdkLoggerProviderBuilder builder);

}
