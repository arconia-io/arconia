package io.arconia.observation.opentelemetry.ai.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.observation.ChatModelMeterObservationHandler;
import org.springframework.ai.embedding.observation.EmbeddingModelMeterObservationHandler;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryAdvisorObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryChatModelMeterObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryEmbeddingMeterObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryEmbeddingModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryImageModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryToolCallingObservationConvention;

/**
 * Conventions and handlers shared by all the flavors, based on the OpenTelemetry GenAI semantic conventions.
 * It's imported after the flavor configurations, so that the conventions a flavor defines take precedence
 * over these defaults.
 */
@Configuration(proxyBeanMethods = false)
class FlavorDefaultsConfiguration {

    @Bean
    @ConditionalOnMissingBean(EmbeddingModelObservationConvention.class)
    OpenTelemetryEmbeddingModelObservationConvention embeddingModelObservationConvention() {
        return new OpenTelemetryEmbeddingModelObservationConvention();
    }

    @Bean
    @ConditionalOnMissingBean(ImageModelObservationConvention.class)
    OpenTelemetryImageModelObservationConvention imageModelObservationConvention() {
        return new OpenTelemetryImageModelObservationConvention();
    }

    @Bean
    @ConditionalOnMissingBean(ToolCallingObservationConvention.class)
    OpenTelemetryToolCallingObservationConvention toolCallingObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        return new OpenTelemetryToolCallingObservationConvention(properties);
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(ChatModelMeterObservationHandler.class)
    OpenTelemetryChatModelMeterObservationHandler chatModelMeterObservationHandler(MeterRegistry meterRegistry) {
        return new OpenTelemetryChatModelMeterObservationHandler(meterRegistry);
    }

    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    @ConditionalOnMissingBean(EmbeddingModelMeterObservationHandler.class)
    OpenTelemetryEmbeddingMeterObservationHandler embeddingModelMeterObservationHandler(MeterRegistry meterRegistry) {
        return new OpenTelemetryEmbeddingMeterObservationHandler(meterRegistry);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ ChatClientObservationConvention.class, AdvisorObservationConvention.class })
    static class ChatClientConventionsConfiguration {

        @Bean
        @ConditionalOnMissingBean(AdvisorObservationConvention.class)
        OpenTelemetryAdvisorObservationConvention advisorObservationConvention() {
            return new OpenTelemetryAdvisorObservationConvention();
        }

    }

}
