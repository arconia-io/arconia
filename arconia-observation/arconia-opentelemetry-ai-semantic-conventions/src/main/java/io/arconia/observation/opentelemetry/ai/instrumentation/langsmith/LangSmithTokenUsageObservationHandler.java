package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;

import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiUsage;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.MicrometerBridge;

/**
 * Observation handler recording the token usage of chat and embedding model calls as numeric span attributes,
 * since LangSmith expects numbers and Micrometer key values are always strings.
 */
public final class LangSmithTokenUsageObservationHandler implements ObservationHandler<Observation.Context> {

    @Override
    public void onStop(Observation.Context context) {
        Usage usage = usage(context);
        if (GenAiUsage.isUnreported(usage)) {
            return;
        }
        Span span = MicrometerBridge.extractOtelSpan(context.get(TracingObservationHandler.TracingContext.class));
        if (span == null) {
            return;
        }
        setTokens(span, GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS.getKey(), usage.getPromptTokens());
        setTokens(span, LangSmithAttributes.GEN_AI_USAGE_TOTAL_TOKENS.getKey(), usage.getTotalTokens());
        if (context instanceof ChatModelObservationContext) {
            setTokens(span, GenAiIncubatingAttributes.GEN_AI_USAGE_OUTPUT_TOKENS.getKey(), usage.getCompletionTokens());
            if (usage.getCacheReadInputTokens() != null) {
                span.setAttribute(GenAiIncubatingAttributes.GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS, usage.getCacheReadInputTokens());
            }
            if (usage.getCacheWriteInputTokens() != null) {
                span.setAttribute(GenAiIncubatingAttributes.GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS, usage.getCacheWriteInputTokens());
            }
        }
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ChatModelObservationContext || context instanceof EmbeddingModelObservationContext;
    }

    private static @Nullable Usage usage(Observation.Context context) {
        if (context instanceof ChatModelObservationContext chatContext && chatContext.getResponse() != null) {
            return chatContext.getResponse().getMetadata().getUsage();
        }
        if (context instanceof EmbeddingModelObservationContext embeddingContext && embeddingContext.getResponse() != null) {
            return embeddingContext.getResponse().getMetadata().getUsage();
        }
        return null;
    }

    private static void setTokens(Span span, String key, @Nullable Integer tokens) {
        if (tokens != null) {
            span.setAttribute(key, tokens.longValue());
        }
    }

}
