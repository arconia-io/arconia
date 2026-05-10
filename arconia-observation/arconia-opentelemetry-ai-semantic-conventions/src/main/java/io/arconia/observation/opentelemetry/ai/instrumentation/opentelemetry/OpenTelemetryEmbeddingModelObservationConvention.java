package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.observation.DefaultEmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.util.StringUtils;

import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

public class OpenTelemetryEmbeddingModelObservationConvention extends DefaultEmbeddingModelObservationConvention {

    @Override
    public String getContextualName(EmbeddingModelObservationContext context) {
        EmbeddingOptions options = context.getRequest().getOptions();
        String operationName = GenAiConventionsConverter.toOperationName(context.getOperationMetadata().operationType());
        if (options != null && StringUtils.hasText(options.getModel())) {
            return "%s %s".formatted(operationName, options.getModel());
        }
        return operationName;
    }

    // LOW CARDINALITY

    @Override
    protected KeyValue aiOperationType(EmbeddingModelObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(),
                GenAiConventionsConverter.toOperationName(context.getOperationMetadata().operationType()));
    }

    @Override
    protected KeyValue aiProvider(EmbeddingModelObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey(),
                GenAiConventionsConverter.toProviderName(context.getOperationMetadata().provider()));
    }

    // HIGH CARDINALITY

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

}
