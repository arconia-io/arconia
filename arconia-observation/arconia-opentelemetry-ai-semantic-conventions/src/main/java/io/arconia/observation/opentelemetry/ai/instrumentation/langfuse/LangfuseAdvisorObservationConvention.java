package io.arconia.observation.opentelemetry.ai.instrumentation.langfuse;

import io.micrometer.common.KeyValues;

import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;

import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryAdvisorObservationConvention;

/**
 * Langfuse flavor of {@link OpenTelemetryAdvisorObservationConvention}.
 *
 * @see <a href="https://langfuse.com/integrations/native/opentelemetry">Langfuse OpenTelemetry</a>
 */
public final class LangfuseAdvisorObservationConvention extends OpenTelemetryAdvisorObservationConvention {

    @Override
    public KeyValues getHighCardinalityKeyValues(AdvisorObservationContext context) {
        return super.getHighCardinalityKeyValues(context).and(LangfuseAttributes.LANGFUSE_OBSERVATION_TYPE, "chain");
    }

}
