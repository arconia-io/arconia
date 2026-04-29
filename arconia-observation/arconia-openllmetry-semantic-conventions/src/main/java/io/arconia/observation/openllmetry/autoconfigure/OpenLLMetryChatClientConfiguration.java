package io.arconia.observation.openllmetry.autoconfigure;

import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.observation.openllmetry.instrumentation.OpenLLMetryAdvisorObservationConvention;
import io.arconia.observation.openllmetry.instrumentation.OpenLLMetryChatClientObservationConvention;

/**
 * Auto-configuration for OpenLLMetry Chat Client instrumentation in Spring AI.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ ChatClientObservationConvention.class, AdvisorObservationConvention.class })
class OpenLLMetryChatClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatClientObservationConvention.class)
    OpenLLMetryChatClientObservationConvention chatClientObservationConvention(OpenLLMetryProperties properties) {
        return new OpenLLMetryChatClientObservationConvention(properties);
    }

    @Bean
    @ConditionalOnMissingBean(AdvisorObservationConvention.class)
    OpenLLMetryAdvisorObservationConvention openLLMetryAdvisorObservationConvention() {
        return new OpenLLMetryAdvisorObservationConvention();
    }

}
