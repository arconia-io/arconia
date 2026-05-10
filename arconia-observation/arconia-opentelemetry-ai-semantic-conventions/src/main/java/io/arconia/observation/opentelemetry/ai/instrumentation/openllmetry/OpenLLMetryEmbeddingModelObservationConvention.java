package io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry;

import io.micrometer.common.KeyValue;

import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;

import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryEmbeddingModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

/**
 * OpenLLMetry flavor of {@link OpenTelemetryEmbeddingModelObservationConvention}.
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 */
public final class OpenLLMetryEmbeddingModelObservationConvention extends OpenTelemetryEmbeddingModelObservationConvention {

    // LOW CARDINALITY

    @Override
    protected KeyValue aiProvider(EmbeddingModelObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM,
                GenAiConventionsConverter.toProviderName(context.getOperationMetadata().provider()));
    }

}
