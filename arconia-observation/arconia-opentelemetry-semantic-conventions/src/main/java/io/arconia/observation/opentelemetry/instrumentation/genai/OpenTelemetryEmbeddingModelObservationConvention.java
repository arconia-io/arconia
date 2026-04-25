package io.arconia.observation.opentelemetry.instrumentation.genai;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.observation.DefaultEmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.util.StringUtils;

public final class OpenTelemetryEmbeddingModelObservationConvention extends DefaultEmbeddingModelObservationConvention {

    private static final KeyValue REQUEST_MODEL_NONE = KeyValue
            .of(GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL.getKey(), KeyValue.NONE_VALUE);

    private static final KeyValue RESPONSE_MODEL_NONE = KeyValue
            .of(GenAiIncubatingAttributes.GEN_AI_RESPONSE_MODEL.getKey(), KeyValue.NONE_VALUE);

    @Override
    public String getContextualName(EmbeddingModelObservationContext context) {
        EmbeddingOptions options = context.getRequest().getOptions();
        String operationName = OpenTelemetryGenAiConventionsConverter.toOperationName(context.getOperationMetadata().operationType());
        if (options != null && StringUtils.hasText(options.getModel())) {
            return "%s %s".formatted(operationName, options.getModel());
        }
        return operationName;
    }

    // LOW CARDINALITY

    @Override
    protected KeyValue aiOperationType(EmbeddingModelObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(),
                OpenTelemetryGenAiConventionsConverter.toOperationName(context.getOperationMetadata().operationType()));
    }

    @Override
    protected KeyValue aiProvider(EmbeddingModelObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey(),
                OpenTelemetryGenAiConventionsConverter.toProviderName(context.getOperationMetadata().provider()));
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

    // Request

    @Override
    protected KeyValues requestEmbeddingDimension(KeyValues keyValues, EmbeddingModelObservationContext context) {
        EmbeddingOptions options = context.getRequest().getOptions();
        if (options != null && options.getDimensions() != null) {
            return keyValues.and(
                    GenAiIncubatingAttributes.GEN_AI_EMBEDDINGS_DIMENSION_COUNT.getKey(),
                    String.valueOf(options.getDimensions()));
        }
        return keyValues;
    }

    // Response

    @Override
    protected KeyValues usageInputTokens(KeyValues keyValues, EmbeddingModelObservationContext context) {
        if (context.getResponse() != null) {
            return keyValues.and(
                    GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS.getKey(),
                    String.valueOf(context.getResponse().getMetadata().getUsage().getPromptTokens()));
        }
        return keyValues;
    }

}
