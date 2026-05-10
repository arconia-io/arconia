package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;

import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryEmbeddingModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

/**
 * LangSmith flavor of {@link OpenTelemetryEmbeddingModelObservationConvention}.
 *
 * @see <a href="https://docs.langchain.com/langsmith/trace-with-opentelemetry">LangSmith</a>
 */
public final class LangSmithEmbeddingModelObservationConvention extends OpenTelemetryEmbeddingModelObservationConvention {

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(EmbeddingModelObservationContext context) {
        return super.getLowCardinalityKeyValues(context).and(langSmithSpanKind(context));
    }

    @Override
    protected KeyValue aiProvider(EmbeddingModelObservationContext context) {
        return KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM.getKey(),
                GenAiConventionsConverter.toProviderName(context.getOperationMetadata().provider()));
    }

    private KeyValue langSmithSpanKind(EmbeddingModelObservationContext context) {
        return KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(),
                GenAiConventionsConverter.toLangSmithSpanKind(context.getOperationMetadata().operationType()));
    }

    // HIGH CARDINALITY

    @Override
    protected KeyValues usageInputTokens(KeyValues keyValues, EmbeddingModelObservationContext context) {
        // LangSmith requires integer input tokens, but Micrometer only supports Strings.
        // Therefore, the token usage is captured via OpenTelemetry APIs directly.
        return keyValues;
    }

    @Override
    protected KeyValues usageTotalTokens(KeyValues keyValues, EmbeddingModelObservationContext context) {
        // LangSmith requires integer total tokens, but Micrometer only supports Strings.
        // Therefore, the token usage is captured via OpenTelemetry APIs directly.
        return keyValues;
    }

}
