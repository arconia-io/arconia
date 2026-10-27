package io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;

import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryAdvisorObservationConvention;

/**
 * OpenLLMetry flavor of {@link OpenTelemetryAdvisorObservationConvention}.
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 */
public final class OpenLLMetryAdvisorObservationConvention extends OpenTelemetryAdvisorObservationConvention {

    @Override
    public KeyValues getLowCardinalityKeyValues(AdvisorObservationContext context) {
        return super.getLowCardinalityKeyValues(context).and(
                traceloopEntityName(context),
                traceloopSpanKind(context)
        );
    }

    private KeyValue traceloopEntityName(AdvisorObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_NAME, context.getAdvisorName());
    }

    private KeyValue traceloopSpanKind(AdvisorObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND,
                OpenLLMetryAttributes.TraceloopSpanKind.TASK.getValue());
    }

}
