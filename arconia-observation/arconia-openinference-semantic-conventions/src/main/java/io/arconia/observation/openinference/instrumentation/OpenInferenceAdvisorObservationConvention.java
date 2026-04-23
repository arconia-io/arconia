package io.arconia.observation.openinference.instrumentation;

import com.arize.semconv.trace.SemanticConventions;

import io.micrometer.common.KeyValue;

import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.advisor.observation.DefaultAdvisorObservationConvention;

/**
 * {@link AdvisorObservationConvention} for OpenInference.
 */
public class OpenInferenceAdvisorObservationConvention extends DefaultAdvisorObservationConvention {

    protected KeyValue aiOperationType(AdvisorObservationContext context) {
        if (context.getAdvisorName().strip().toLowerCase().contains("guardrail")) {
            return KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, SemanticConventions.OpenInferenceSpanKind.GUARDRAIL.getValue());
        }
        return KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, SemanticConventions.OpenInferenceSpanKind.CHAIN.getValue());
    }

}
