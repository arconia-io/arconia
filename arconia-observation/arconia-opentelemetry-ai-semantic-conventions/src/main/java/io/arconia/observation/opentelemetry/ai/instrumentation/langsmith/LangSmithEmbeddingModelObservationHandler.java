package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;

import io.arconia.observation.opentelemetry.ai.instrumentation.shared.MicrometerBridge;

/**
 * Observation handler that sets typed span attributes for embedding model observations.
 * <p>
 * This handler sets token usage attributes with proper numeric types directly
 * on the OTel Span (Micrometer KeyValues are always strings, but LangSmith
 * expects numeric types for usage metrics).
 */
public final class LangSmithEmbeddingModelObservationHandler implements ObservationHandler<EmbeddingModelObservationContext> {

    @Override
    public void onStop(EmbeddingModelObservationContext context) {
        TracingObservationHandler.TracingContext tracingContext = context
                .get(TracingObservationHandler.TracingContext.class);
        Span span = MicrometerBridge.extractOtelSpan(tracingContext);
        if (span == null || context.getResponse() == null) {
            return;
        }
        var usage = context.getResponse().getMetadata().getUsage();
        span.setAttribute(GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS, Long.valueOf(usage.getPromptTokens()));
        span.setAttribute(LangSmithAttributes.GEN_AI_USAGE_TOTAL_TOKENS.getKey(),
                Long.valueOf(usage.getTotalTokens()));
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof EmbeddingModelObservationContext;
    }

}
