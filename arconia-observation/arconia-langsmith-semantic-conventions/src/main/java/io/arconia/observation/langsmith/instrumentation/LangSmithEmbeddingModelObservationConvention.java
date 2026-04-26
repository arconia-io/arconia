package io.arconia.observation.langsmith.instrumentation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.observation.DefaultEmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.util.StringUtils;

public final class LangSmithEmbeddingModelObservationConvention extends DefaultEmbeddingModelObservationConvention {

    private static final KeyValue REQUEST_MODEL_NONE = KeyValue
            .of(GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL.getKey(), KeyValue.NONE_VALUE);

    private static final KeyValue RESPONSE_MODEL_NONE = KeyValue
            .of(GenAiIncubatingAttributes.GEN_AI_RESPONSE_MODEL.getKey(), KeyValue.NONE_VALUE);

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

    @Override
    protected KeyValue requestModel(EmbeddingModelObservationContext context) {
        EmbeddingOptions options = context.getRequest().getOptions();
        if (options != null && StringUtils.hasText(options.getModel())) {
            return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL.getKey(),
                    options.getModel());
        }
        return REQUEST_MODEL_NONE;
    }

    @Override
    protected KeyValue responseModel(EmbeddingModelObservationContext context) {
        if (context.getResponse() != null && StringUtils.hasText(context.getResponse().getMetadata().getModel())) {
            return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_RESPONSE_MODEL.getKey(),
                    context.getResponse().getMetadata().getModel());
        }
        return RESPONSE_MODEL_NONE;
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(EmbeddingModelObservationContext context) {
        return KeyValues.empty();
    }

}
