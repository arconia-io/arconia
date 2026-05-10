package io.arconia.observation.opentelemetry.ai.autoconfigure;

import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryAdvisorObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryEmbeddingModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryToolCallingObservationConvention;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = OpenTelemetryAiConventionsProperties.CONFIG_PREFIX, name = "flavor", havingValue = "openllmetry")
class OpenLLMetryFlavorConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatModelObservationConvention.class)
    OpenLLMetryChatModelObservationConvention chatModelObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        return new OpenLLMetryChatModelObservationConvention(properties);
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModelObservationConvention.class)
    OpenLLMetryEmbeddingModelObservationConvention embeddingModelObservationConvention() {
        return new OpenLLMetryEmbeddingModelObservationConvention();
    }

    @Bean
    @ConditionalOnMissingBean(ToolCallingObservationConvention.class)
    OpenLLMetryToolCallingObservationConvention toolCallingObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        return new OpenLLMetryToolCallingObservationConvention(properties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ ChatClientObservationConvention.class, AdvisorObservationConvention.class })
    static class ChatClientConventionsConfiguration {

        @Bean
        @ConditionalOnMissingBean(ChatClientObservationConvention.class)
        OpenLLMetryChatClientObservationConvention chatClientObservationConvention(OpenTelemetryAiConventionsProperties properties) {
            return new OpenLLMetryChatClientObservationConvention(properties);
        }

        @Bean
        @ConditionalOnMissingBean(AdvisorObservationConvention.class)
        OpenLLMetryAdvisorObservationConvention advisorObservationConvention() {
            return new OpenLLMetryAdvisorObservationConvention();
        }

    }

}
