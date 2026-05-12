package io.arconia.observation.opentelemetry.ai.instrumentation.openlit;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;

import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryEmbeddingModelObservationConvention;

/**
 * OpenLIT flavor of {@link OpenTelemetryEmbeddingModelObservationConvention}.
 *
 * @see <a href="https://github.com/openlit/openlit">OpenLIT</a>
 */
public final class OpenLitEmbeddingModelObservationConvention extends OpenTelemetryEmbeddingModelObservationConvention {

    @Override
    protected KeyValue aiProvider(EmbeddingModelObservationContext context) {
        return KeyValue.of(OpenLitAttributes.GEN_AI_SYSTEM,
                GenAiConventionsConverter.toOpenLitProviderName(context.getOperationMetadata().provider()));
    }

    @Override
    protected KeyValues requestEmbeddingDimension(KeyValues keyValues, EmbeddingModelObservationContext context) {
        EmbeddingOptions options = context.getRequest().getOptions();
        if (options != null && options.getDimensions() != null) {
            return keyValues.and(OpenLitAttributes.GEN_AI_REQUEST_EMBEDDING_DIMENSION,
                    String.valueOf(options.getDimensions()));
        }
        return keyValues;
    }

}
