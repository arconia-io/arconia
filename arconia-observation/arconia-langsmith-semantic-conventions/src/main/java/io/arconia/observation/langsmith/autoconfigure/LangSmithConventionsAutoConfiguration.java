package io.arconia.observation.langsmith.autoconfigure;

import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.observation.autoconfigure.ObservationProperties;
import io.arconia.observation.conventions.ObservationConventionsProvider;
import io.arconia.observation.langsmith.instrumentation.LangSmithAdvisorObservationConvention;
import io.arconia.observation.langsmith.instrumentation.LangSmithChatClientObservationConvention;
import io.arconia.observation.langsmith.instrumentation.LangSmithChatClientObservationHandler;
import io.arconia.observation.langsmith.instrumentation.LangSmithChatModelObservationConvention;
import io.arconia.observation.langsmith.instrumentation.LangSmithChatModelObservationHandler;
import io.arconia.observation.langsmith.instrumentation.LangSmithEmbeddingModelObservationConvention;
import io.arconia.observation.langsmith.instrumentation.LangSmithEmbeddingModelObservationHandler;
import io.arconia.observation.langsmith.instrumentation.LangSmithToolCallingObservationConvention;

/**
 * Auto-configuration for LangSmith Semantic Conventions.
 *
 * @see <a href="https://docs.langchain.com/langsmith/trace-with-opentelemetry">LangSmith OpenTelemetry Tracing</a>
 */
@AutoConfiguration(beforeName = {
        "org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration",
        "org.springframework.ai.model.chat.observation.autoconfigure.ChatObservationAutoConfiguration",
        "org.springframework.ai.model.embedding.observation.autoconfigure.EmbeddingObservationAutoConfiguration",
        "org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration",
})
@ConditionalOnClass({ChatModelObservationConvention.class, EmbeddingModelObservationConvention.class, ToolCallingObservationConvention.class})
@ConditionalOnProperty(prefix = ObservationProperties.CONFIG_PREFIX, name = "conventions.type", havingValue = "langsmith", matchIfMissing = true)
@ConditionalOnBooleanProperty(prefix = LangSmithConventionsProperties.CONFIG_PREFIX, value = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(LangSmithConventionsProperties.class)
public final class LangSmithConventionsAutoConfiguration {

    @Bean
    ObservationConventionsProvider langSmithConventionsProvider() {
        return () -> "langsmith";
    }

    @Bean
    @ConditionalOnMissingBean(ChatModelObservationConvention.class)
    LangSmithChatModelObservationConvention chatModelObservationConvention(LangSmithConventionsProperties properties) {
        return new LangSmithChatModelObservationConvention(properties);
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModelObservationConvention.class)
    LangSmithEmbeddingModelObservationConvention embeddingModelObservationConvention() {
        return new LangSmithEmbeddingModelObservationConvention();
    }

    @Bean
    @ConditionalOnMissingBean(ToolCallingObservationConvention.class)
    LangSmithToolCallingObservationConvention toolCallingObservationConvention(LangSmithConventionsProperties properties) {
        return new LangSmithToolCallingObservationConvention(properties);
    }

    @Bean
    @ConditionalOnMissingBean(LangSmithChatModelObservationHandler.class)
    @ConditionalOnClass(name = "io.micrometer.tracing.otel.bridge.OtelSpan")
    LangSmithChatModelObservationHandler chatModelObservationHandler(LangSmithConventionsProperties properties) {
        return new LangSmithChatModelObservationHandler(properties);
    }

    @Bean
    @ConditionalOnMissingBean(LangSmithEmbeddingModelObservationHandler.class)
    @ConditionalOnClass(name = "io.micrometer.tracing.otel.bridge.OtelSpan")
    LangSmithEmbeddingModelObservationHandler embeddingModelObservationHandler() {
        return new LangSmithEmbeddingModelObservationHandler();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ChatClientObservationConvention.class, AdvisorObservationConvention.class})
    static class ChatClientConventionsConfiguration {

        @Bean
        @ConditionalOnMissingBean(ChatClientObservationConvention.class)
        LangSmithChatClientObservationConvention chatClientObservationConvention() {
            return new LangSmithChatClientObservationConvention();
        }

        @Bean
        @ConditionalOnMissingBean(LangSmithChatClientObservationHandler.class)
        @ConditionalOnClass(name = "io.micrometer.tracing.otel.bridge.OtelSpan")
        LangSmithChatClientObservationHandler chatClientObservationHandler(LangSmithConventionsProperties properties) {
            return new LangSmithChatClientObservationHandler(properties);
        }

        @Bean
        @ConditionalOnMissingBean(AdvisorObservationConvention.class)
        LangSmithAdvisorObservationConvention advisorObservationConvention() {
            return new LangSmithAdvisorObservationConvention();
        }

    }

}
