package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.image.observation.ImageModelObservationContext;

import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryImageModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

/**
 * LangSmith flavor of {@link OpenTelemetryImageModelObservationConvention}.
 *
 * @see <a href="https://docs.langchain.com/langsmith/trace-with-opentelemetry">LangSmith</a>
 */
public final class LangSmithImageModelObservationConvention extends OpenTelemetryImageModelObservationConvention {

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(ImageModelObservationContext context) {
        return super.getLowCardinalityKeyValues(context).and(langSmithSpanKind(context));
    }

    @Override
    protected KeyValue aiProvider(ImageModelObservationContext context) {
        return KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM.getKey(),
                GenAiConventionsConverter.toLangSmithProviderName(context.getOperationMetadata().provider()));
    }

    private KeyValue langSmithSpanKind(ImageModelObservationContext context) {
        return KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(),
                GenAiConventionsConverter.toLangSmithSpanKind(context.getOperationMetadata().operationType()));
    }

}
