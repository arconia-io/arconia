package io.arconia.observation.langsmith.instrumentation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.observation.DefaultEmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.util.StringUtils;

public final class LangSmithEmbeddingModelObservationConvention extends DefaultEmbeddingModelObservationConvention {

    @Override
    public String getContextualName(EmbeddingModelObservationContext context) {
        EmbeddingOptions options = context.getRequest().getOptions();
        String operationName = LangSmithConventionsConverter.toOperationName(context.getOperationMetadata().operationType());
        if (options != null && StringUtils.hasText(options.getModel())) {
            return "%s %s".formatted(operationName, options.getModel());
        }
        return operationName;
    }

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(EmbeddingModelObservationContext context) {
        return KeyValues.of(aiOperationType(context), aiProvider(context), langSmithSpanKind(context),
                requestModel(context), responseModel(context));
    }

    @Override
    protected KeyValue aiOperationType(EmbeddingModelObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(),
                LangSmithConventionsConverter.toOperationName(context.getOperationMetadata().operationType()));
    }

    @Override
    protected KeyValue aiProvider(EmbeddingModelObservationContext context) {
        return KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM.getKey(),
                LangSmithConventionsConverter.toSystemName(context.getOperationMetadata().provider()));
    }

    private KeyValue langSmithSpanKind(EmbeddingModelObservationContext context) {
        return KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(),
                LangSmithConventionsConverter.toSpanKind(context.getOperationMetadata().operationType()));
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(EmbeddingModelObservationContext context) {
        return KeyValues.empty();
    }

}
