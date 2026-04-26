package io.arconia.observation.langsmith.instrumentation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.chat.client.advisor.observation.DefaultAdvisorObservationConvention;
import org.springframework.ai.observation.conventions.AiOperationType;

public final class LangSmithAdvisorObservationConvention extends DefaultAdvisorObservationConvention {

    // LOW CARDINALITY

    @Override
    public KeyValues getLowCardinalityKeyValues(AdvisorObservationContext context) {
        return KeyValues.of(aiOperationType(context), aiProvider(context), langSmithSpanKind());
    }

    @Override
    protected KeyValue aiOperationType(AdvisorObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), AiOperationType.FRAMEWORK.value());
    }

    private KeyValue langSmithSpanKind() {
        return KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(), "chain");
    }

    // HIGH CARDINALITY

    @Override
    public KeyValues getHighCardinalityKeyValues(AdvisorObservationContext context) {
        return KeyValues.empty();
    }

}
