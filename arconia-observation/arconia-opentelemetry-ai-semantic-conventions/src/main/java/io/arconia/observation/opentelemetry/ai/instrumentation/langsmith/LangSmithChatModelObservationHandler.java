package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.chat.observation.ChatModelObservationContext;

import io.arconia.observation.opentelemetry.ai.instrumentation.shared.MicrometerBridge;

/**
 * Observation handler that sets typed span attributes for chat model observations.
 * <p>
 * This handler sets token usage attributes with proper numeric types directly
 * on the OTel Span (Micrometer KeyValues are always strings, but LangSmith
 * expects numeric types for usage metrics).
 */
public final class LangSmithChatModelObservationHandler implements ObservationHandler<ChatModelObservationContext> {

    @Override
    public void onStop(ChatModelObservationContext context) {
        TracingObservationHandler.TracingContext tracingContext = context
                .get(TracingObservationHandler.TracingContext.class);
        Span span = MicrometerBridge.extractOtelSpan(tracingContext);
        if (span == null || context.getResponse() == null) {
            return;
        }
        var usage = context.getResponse().getMetadata().getUsage();
        span.setAttribute(GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS, Long.valueOf(usage.getPromptTokens()));
        span.setAttribute(GenAiIncubatingAttributes.GEN_AI_USAGE_OUTPUT_TOKENS,
                Long.valueOf(usage.getCompletionTokens()));
        span.setAttribute(LangSmithAttributes.GEN_AI_USAGE_TOTAL_TOKENS.getKey(),
                Long.valueOf(usage.getTotalTokens()));
        if (usage.getCacheReadInputTokens() != null) {
            span.setAttribute(GenAiIncubatingAttributes.GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS,
                    usage.getCacheReadInputTokens());
        }
        if (usage.getCacheWriteInputTokens() != null) {
            span.setAttribute(GenAiIncubatingAttributes.GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS,
                    usage.getCacheWriteInputTokens());
        }
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatModelObservationContext;
    }

}
