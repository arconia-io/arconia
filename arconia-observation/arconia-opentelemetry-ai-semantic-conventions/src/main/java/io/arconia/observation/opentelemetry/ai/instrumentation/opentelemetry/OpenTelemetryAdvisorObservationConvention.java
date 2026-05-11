package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import io.micrometer.common.KeyValue;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.chat.client.advisor.observation.DefaultAdvisorObservationConvention;
import org.springframework.ai.observation.conventions.AiProvider;

public class OpenTelemetryAdvisorObservationConvention extends DefaultAdvisorObservationConvention {

    @Override
    protected KeyValue aiProvider(AdvisorObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey(), AiProvider.SPRING_AI.value());
    }

}
