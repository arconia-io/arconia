package io.arconia.openinference.observation.autoconfigure.ai;

import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.openinference.observation.instrumentation.ai.OpenInferenceAdvisorObservationConvention;
import io.arconia.openinference.observation.instrumentation.ai.OpenInferenceChatClientObservationConvention;

/**
 * Auto-configuration for OpenInference Chat Client instrumentation in Spring AI.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ ChatClientObservationConvention.class, AdvisorObservationConvention.class })
class OpenInferenceChatClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatClientObservationConvention.class)
    OpenInferenceChatClientObservationConvention chatClientObservationConvention(OpenInferenceProperties properties) {
        return new OpenInferenceChatClientObservationConvention(properties.getTraces());
    }

    @Bean
    @ConditionalOnMissingBean(AdvisorObservationConvention.class)
    OpenInferenceAdvisorObservationConvention openInferenceAdvisorObservationConvention() {
        return new OpenInferenceAdvisorObservationConvention();
    }

}
