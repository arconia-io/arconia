package io.arconia.observation.openllmetry.instrumentation;

import io.micrometer.common.KeyValue;

import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.advisor.observation.DefaultAdvisorObservationConvention;

/**
 * {@link AdvisorObservationConvention} for OpenLLMetry.
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 */
public class OpenLLMetryAdvisorObservationConvention extends DefaultAdvisorObservationConvention {

    protected KeyValue aiOperationType(AdvisorObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, OpenLLMetryAttributes.SPAN_KIND_TASK);
    }

}
