package io.arconia.observation.opentelemetry.instrumentation.genai;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.observation.EmbeddingModelMeterObservationHandler;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;

public final class OpenTelemetryEmbeddingModelMeterObservationHandler extends EmbeddingModelMeterObservationHandler {

    private final MeterRegistry meterRegistry;

    public OpenTelemetryEmbeddingModelMeterObservationHandler(MeterRegistry meterRegistry) {
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
