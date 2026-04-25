package io.arconia.observation.opentelemetry.instrumentation.genai;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.observation.ChatModelMeterObservationHandler;
import org.springframework.ai.chat.observation.ChatModelObservationContext;

public final class OpenTelemetryChatModelMeterObservationHandler extends ChatModelMeterObservationHandler {

    private final MeterRegistry meterRegistry;

    public OpenTelemetryChatModelMeterObservationHandler(MeterRegistry meterRegistry) {
        super(meterRegistry);
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void onStop(ChatModelObservationContext context) {
        if (context.getResponse() != null) {
            Usage usage = context.getResponse().getMetadata().getUsage();
            OpenTelemetryTokenUsageMetricsGenerator.inputTokens(usage, context, meterRegistry);
            OpenTelemetryTokenUsageMetricsGenerator.outputTokens(usage, context, meterRegistry);
        }
    }

}
