package io.arconia.observation.openllmetry.autoconfigure;

import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.model.observation.ModelObservationContext;
import org.springframework.ai.tool.observation.ToolCallingObservationConvention;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.arconia.observation.autoconfigure.ObservationProperties;
import io.arconia.observation.conventions.ObservationConventionsProvider;
import io.arconia.observation.openllmetry.instrumentation.OpenLLMetryChatModelObservationConvention;
import io.arconia.observation.openllmetry.instrumentation.OpenLLMetryEmbeddingModelObservationConvention;
import io.arconia.observation.openllmetry.instrumentation.OpenLLMetryToolCallingObservationConvention;

/**
 * Auto-configuration for OpenLLMetry instrumentation in Spring AI.
 */
@AutoConfiguration(beforeName = {
        "org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration",
        "org.springframework.ai.model.chat.observation.autoconfigure.ChatObservationAutoConfiguration",
        "org.springframework.ai.model.embedding.observation.autoconfigure.EmbeddingObservationAutoConfiguration",
        "org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration",
})
@ConditionalOnClass(ModelObservationContext.class)
@ConditionalOnProperty(prefix = ObservationProperties.CONFIG_PREFIX, name = "conventions.type", havingValue = "openllmetry", matchIfMissing = true)
@EnableConfigurationProperties(OpenLLMetryProperties.class)
@Import(OpenLLMetryChatClientConfiguration.class)
public final class OpenLLMetryAutoConfiguration {

    @Bean
    ObservationConventionsProvider openLLMetryConventionsProvider() {
        return () -> "openllmetry";
    }

    @Bean
    @ConditionalOnMissingBean(ChatModelObservationConvention.class)
    OpenLLMetryChatModelObservationConvention chatModelObservationConvention(OpenLLMetryProperties properties) {
        return new OpenLLMetryChatModelObservationConvention(properties);
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModelObservationConvention.class)
    OpenLLMetryEmbeddingModelObservationConvention embeddingModelObservationConvention(OpenLLMetryProperties properties) {
        return new OpenLLMetryEmbeddingModelObservationConvention(properties);
    }

    @Bean
    @ConditionalOnMissingBean(ToolCallingObservationConvention.class)
    OpenLLMetryToolCallingObservationConvention toolCallingObservationConvention(OpenLLMetryProperties properties) {
        return new OpenLLMetryToolCallingObservationConvention(properties);
    }

}
