package io.arconia.observation.opentelemetry.ai.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.observation.ChatModelMeterObservationHandler;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelMeterObservationHandler;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryAdvisorObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry.OpenLLMetryToolCallingObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelMeterObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryEmbeddingMeterObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryEmbeddingModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryImageModelObservationConvention;

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
    OpenTelemetryEmbeddingModelObservationConvention embeddingModelObservationConvention() {
        return new OpenTelemetryEmbeddingModelObservationConvention();
    }

    @Bean
    @ConditionalOnMissingBean(ToolCallingObservationConvention.class)
    OpenLLMetryToolCallingObservationConvention toolCallingObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        return new OpenLLMetryToolCallingObservationConvention(properties);
    }

    @Bean
    @ConditionalOnClass(ImageModelObservationConvention.class)
    @ConditionalOnMissingBean(ImageModelObservationConvention.class)
    OpenTelemetryImageModelObservationConvention imageModelObservationConvention() {
        return new OpenTelemetryImageModelObservationConvention();
    }

    @Bean
    @ConditionalOnMissingBean(ChatModelMeterObservationHandler.class)
    OpenTelemetryChatModelMeterObservationHandler chatModelMeterObservationHandler(ObjectProvider<MeterRegistry> meterRegistry) {
        return new OpenTelemetryChatModelMeterObservationHandler(meterRegistry.getObject());
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModelMeterObservationHandler.class)
    OpenTelemetryEmbeddingMeterObservationHandler embeddingModelMeterObservationHandler(ObjectProvider<MeterRegistry> meterRegistry) {
        return new OpenTelemetryEmbeddingMeterObservationHandler(meterRegistry.getObject());
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
