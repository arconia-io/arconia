package io.arconia.observation.opentelemetry.ai.instrumentation.openlit;

import io.micrometer.common.KeyValue;

import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.observation.conventions.AiProvider;

import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryAdvisorObservationConvention;

/**
 * OpenLIT flavor of {@link OpenTelemetryAdvisorObservationConvention}.
 *
 * @see <a href="https://github.com/openlit/openlit">OpenLIT</a>
 */
public final class OpenLitAdvisorObservationConvention extends OpenTelemetryAdvisorObservationConvention {

    @Override
    protected KeyValue aiProvider(AdvisorObservationContext context) {
        return KeyValue.of(OpenLitAttributes.GEN_AI_SYSTEM, AiProvider.SPRING_AI.value());
    }

}
