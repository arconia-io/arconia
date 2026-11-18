package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.image.observation.ImageModelObservationContext;

import io.arconia.core.annotations.Incubating;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryImageModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

/**
 * LangSmith flavor of {@link OpenTelemetryImageModelObservationConvention}.
 *
 * @see <a href="https://docs.langchain.com/langsmith/trace-with-opentelemetry">LangSmith</a>
 */
@Incubating
public final class LangSmithImageModelObservationConvention extends OpenTelemetryImageModelObservationConvention {

    // LOW CARDINALITY

    @Override
    protected KeyValue aiProvider(ImageModelObservationContext context) {
        return KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM.getKey(),
                GenAiConventionsConverter.toLangSmithProviderName(context.getOperationMetadata().provider()));
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(ImageModelObservationContext context) {
        return super.getHighCardinalityKeyValues(context).and(langSmithSpanKind(context));
    }

    private KeyValue langSmithSpanKind(ImageModelObservationContext context) {
        return KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(),
                GenAiConventionsConverter.toLangSmithSpanKind(context.getOperationMetadata().operationType()));
    }

}
