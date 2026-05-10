package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.observation.EmbeddingModelMeterObservationHandler;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;

public final class OpenTelemetryEmbeddingMeterObservationHandler extends EmbeddingModelMeterObservationHandler {

    private final MeterRegistry meterRegistry;

    public OpenTelemetryEmbeddingMeterObservationHandler(MeterRegistry meterRegistry) {
        super(meterRegistry);
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void onStop(EmbeddingModelObservationContext context) {
        if (context.getResponse() != null) {
            Usage usage = context.getResponse().getMetadata().getUsage();
            OpenTelemetryTokenUsageMetricsGenerator.inputTokens(usage, context, meterRegistry);
        }
    }

}
