package io.arconia.observation.opentelemetry.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.observation.ChatModelMeterObservationHandler;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelMeterObservationHandler;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.observation.autoconfigure.ObservationProperties;
import io.arconia.observation.opentelemetry.instrumentation.genai.OpenTelemetryChatClientObservationConvention;
import io.arconia.observation.opentelemetry.instrumentation.genai.OpenTelemetryChatModelEventObservationHandler;
import io.arconia.observation.opentelemetry.instrumentation.genai.OpenTelemetryChatModelMeterObservationHandler;
import io.arconia.observation.opentelemetry.instrumentation.genai.OpenTelemetryChatModelObservationConvention;
import io.arconia.observation.opentelemetry.instrumentation.genai.OpenTelemetryEmbeddingModelMeterObservationHandler;
import io.arconia.observation.opentelemetry.instrumentation.genai.OpenTelemetryEmbeddingModelObservationConvention;
import io.arconia.observation.opentelemetry.instrumentation.genai.OpenTelemetryToolCallingModelObservationConvention;

/**
 * Auto-configuration for OpenTelemetry Semantic Conventions for Generative AI.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai/">OpenTelemetry Semantic Conventions for Generative AI</a>
 */
@AutoConfiguration(beforeName = {
        "org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration",
        "org.springframework.ai.model.chat.observation.autoconfigure.ChatObservationAutoConfiguration",
        "org.springframework.ai.model.embedding.observation.autoconfigure.EmbeddingObservationAutoConfiguration",
        "org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration",
})
@ConditionalOnClass({ChatModelObservationConvention.class, EmbeddingModelObservationConvention.class, ToolCallingObservationConvention.class})
@ConditionalOnProperty(prefix = ObservationProperties.CONFIG_PREFIX, name = "conventions.type", havingValue = "opentelemetry", matchIfMissing = true)
@ConditionalOnBooleanProperty(prefix = OpenTelemetryConventionsProperties.CONFIG_PREFIX, value = "generative-ai.enabled", matchIfMissing = true)
@EnableConfigurationProperties(OpenTelemetryConventionsProperties.class)
public final class GenerativeAiConventionsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatModelObservationConvention.class)
    OpenTelemetryChatModelObservationConvention chatModelObservationConvention(OpenTelemetryConventionsProperties properties) {
        return new OpenTelemetryChatModelObservationConvention(properties.getGenerativeAi());
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModelObservationConvention.class)
    OpenTelemetryEmbeddingModelObservationConvention embeddingModelObservationConvention() {
        return new OpenTelemetryEmbeddingModelObservationConvention();
    }

    @Bean
    @ConditionalOnMissingBean(ToolCallingObservationConvention.class)
    OpenTelemetryToolCallingModelObservationConvention toolCallingObservationConvention(OpenTelemetryConventionsProperties properties) {
        return new OpenTelemetryToolCallingModelObservationConvention(properties.getGenerativeAi());
    }

    @Bean
    @ConditionalOnMissingBean(ChatModelMeterObservationHandler.class)
    OpenTelemetryChatModelMeterObservationHandler chatModelMeterObservationHandler(ObjectProvider<MeterRegistry> meterRegistry) {
        return new OpenTelemetryChatModelMeterObservationHandler(meterRegistry.getObject());
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModelMeterObservationHandler.class)
    OpenTelemetryEmbeddingModelMeterObservationHandler embeddingModelMeterObservationHandler(ObjectProvider<MeterRegistry> meterRegistry) {
        return new OpenTelemetryEmbeddingModelMeterObservationHandler(meterRegistry.getObject());
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "io.micrometer.tracing.otel.bridge.OtelSpan")
    OpenTelemetryChatModelEventObservationHandler chatModelEventObservationHandler(OpenTelemetryConventionsProperties properties) {
        return new OpenTelemetryChatModelEventObservationHandler(properties.getGenerativeAi());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ChatClientObservationConvention.class, AdvisorObservationConvention.class})
    static class ChatClientConventionsConfiguration {

        @Bean
        @ConditionalOnMissingBean(ChatClientObservationConvention.class)
        OpenTelemetryChatClientObservationConvention chatClientObservationConvention() {
            return new OpenTelemetryChatClientObservationConvention();
        }

    }

}
