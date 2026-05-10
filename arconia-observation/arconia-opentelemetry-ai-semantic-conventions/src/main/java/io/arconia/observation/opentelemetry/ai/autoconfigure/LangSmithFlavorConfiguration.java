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

import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithAiAdvisorConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithChatClientObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithChatModelObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithEmbeddingModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithEmbeddingModelObservationHandler;
import io.arconia.observation.opentelemetry.ai.instrumentation.langsmith.LangSmithToolCallingObservationConvention;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = OpenTelemetryAiConventionsProperties.CONFIG_PREFIX, name = "flavor", havingValue = "langsmith")
class LangSmithFlavorConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatModelObservationConvention.class)
    LangSmithChatModelObservationConvention chatModelObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        return new LangSmithChatModelObservationConvention(properties);
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModelObservationConvention.class)
    LangSmithEmbeddingModelObservationConvention embeddingModelObservationConvention() {
        return new LangSmithEmbeddingModelObservationConvention();
    }

    @Bean
    @ConditionalOnMissingBean(ToolCallingObservationConvention.class)
    LangSmithToolCallingObservationConvention toolCallingObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        return new LangSmithToolCallingObservationConvention(properties);
    }

    @Bean
    @ConditionalOnMissingBean(LangSmithChatModelObservationHandler.class)
    @ConditionalOnClass(name = "io.micrometer.tracing.otel.bridge.OtelSpan")
    LangSmithChatModelObservationHandler chatModelObservationHandler(OpenTelemetryAiConventionsProperties properties) {
        return new LangSmithChatModelObservationHandler(properties);
    }

    @Bean
    @ConditionalOnMissingBean(LangSmithEmbeddingModelObservationHandler.class)
    @ConditionalOnClass(name = "io.micrometer.tracing.otel.bridge.OtelSpan")
    LangSmithEmbeddingModelObservationHandler embeddingModelObservationHandler() {
        return new LangSmithEmbeddingModelObservationHandler();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ ChatClientObservationConvention.class, AdvisorObservationConvention.class })
    static class ChatClientConventionsConfiguration {

        @Bean
        @ConditionalOnMissingBean(ChatClientObservationConvention.class)
        LangSmithChatClientObservationConvention chatClientObservationConvention(OpenTelemetryAiConventionsProperties properties) {
            return new LangSmithChatClientObservationConvention(properties);
        }

        @Bean
        @ConditionalOnMissingBean(LangSmithChatClientObservationHandler.class)
        @ConditionalOnClass(name = "io.micrometer.tracing.otel.bridge.OtelSpan")
        LangSmithChatClientObservationHandler chatClientObservationHandler(OpenTelemetryAiConventionsProperties properties) {
            return new LangSmithChatClientObservationHandler(properties);
        }

        @Bean
        @ConditionalOnMissingBean(AdvisorObservationConvention.class)
        LangSmithAiAdvisorConvention advisorObservationConvention() {
            return new LangSmithAiAdvisorConvention();
        }

    }

}
