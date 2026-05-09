package io.arconia.observation.openllmetry.instrumentation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.observation.DefaultEmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;

/**
 * {@link EmbeddingModelObservationConvention} for OpenLLMetry.
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 */
public final class OpenLLMetryEmbeddingModelObservationConvention extends DefaultEmbeddingModelObservationConvention {

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(EmbeddingModelObservationContext context) {
        return KeyValues.of(aiOperationType(context), aiProvider(context), requestModel(context),
                responseModel(context), traceloopSpanKind());
    }

    @Override
    protected KeyValue aiOperationType(EmbeddingModelObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(),
                OpenLLMetryConventionsConverters.toOperationName(context.getOperationMetadata().operationType()));
    }

    @Override
    protected KeyValue aiProvider(EmbeddingModelObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM,
                OpenLLMetryConventionsConverters.toSystemName(context.getOperationMetadata().provider()));
    }

    private KeyValue traceloopSpanKind() {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, OpenLLMetryAttributes.TraceloopSpanKind.TASK.getValue());
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

}
