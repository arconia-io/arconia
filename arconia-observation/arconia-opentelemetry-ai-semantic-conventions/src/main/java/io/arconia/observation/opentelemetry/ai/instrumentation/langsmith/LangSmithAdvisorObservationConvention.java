package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.observation.conventions.AiProvider;

import io.arconia.core.annotations.Incubating;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryAdvisorObservationConvention;

/**
 * LangSmith flavor of {@link OpenTelemetryAdvisorObservationConvention}.
 *
 * @see <a href="https://docs.langchain.com/langsmith/trace-with-opentelemetry">LangSmith</a>
 */
@Incubating
public final class LangSmithAdvisorObservationConvention extends OpenTelemetryAdvisorObservationConvention {

    @Override
    public KeyValues getHighCardinalityKeyValues(AdvisorObservationContext context) {
        return super.getHighCardinalityKeyValues(context).and(langSmithSpanKind(context));
    }

    @Override
    protected KeyValue aiProvider(AdvisorObservationContext context) {
        return KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM, AiProvider.SPRING_AI.value());
    }

    private KeyValue langSmithSpanKind(AdvisorObservationContext context) {
        return KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND, "chain");
    }

}
