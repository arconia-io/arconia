package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;

import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiUsage;
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
        if (!GenAiUsage.isReported(usage)) {
            return;
        }
        setTokens(span, GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS.getKey(), usage.getPromptTokens());
        setTokens(span, LangSmithAttributes.GEN_AI_USAGE_TOTAL_TOKENS.getKey(), usage.getTotalTokens());
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof EmbeddingModelObservationContext;
    }

    /**
     * Sets a token count on the span, skipping counts the usage doesn't carry.
     */
    private static void setTokens(Span span, String key, @Nullable Integer tokens) {
        if (tokens != null) {
            span.setAttribute(key, tokens.longValue());
        }
    }

}
