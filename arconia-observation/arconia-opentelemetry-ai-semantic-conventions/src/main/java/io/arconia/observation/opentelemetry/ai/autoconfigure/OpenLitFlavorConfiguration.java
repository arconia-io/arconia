package io.arconia.observation.opentelemetry.ai.autoconfigure;

import java.util.List;

import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationConvention;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.observation.opentelemetry.ai.instrumentation.openlit.OpenLitChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openlit.OpenLitChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openlit.OpenLitEmbeddingModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.openlit.OpenLitImageModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConversationIdObservationHandler;

/**
 * Conventions of the {@code openlit} flavor, on top of the shared OpenTelemetry GenAI ones.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = OpenTelemetryAiConventionsProperties.CONFIG_PREFIX, name = "flavor", havingValue = "openlit")
class OpenLitFlavorConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatModelObservationConvention.class)
    OpenLitChatModelObservationConvention chatModelObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        return new OpenLitChatModelObservationConvention(properties);
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingModelObservationConvention.class)
    OpenLitEmbeddingModelObservationConvention embeddingModelObservationConvention() {
        return new OpenLitEmbeddingModelObservationConvention();
    }

    @Bean
    @ConditionalOnMissingBean(ImageModelObservationConvention.class)
    OpenLitImageModelObservationConvention imageModelObservationConvention() {
        return new OpenLitImageModelObservationConvention();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ ChatClientObservationConvention.class, AdvisorObservationConvention.class })
    static class ChatClientConventionsConfiguration {

        @Bean
        @ConditionalOnMissingBean(ChatClientObservationConvention.class)
        OpenLitChatClientObservationConvention chatClientObservationConvention(OpenTelemetryAiConventionsProperties properties) {
            return new OpenLitChatClientObservationConvention(properties);
        }

        @Bean
        @ConditionalOnMissingBean(GenAiConversationIdObservationHandler.class)
        GenAiConversationIdObservationHandler conversationIdObservationHandler() {
            return new GenAiConversationIdObservationHandler(List.of(GenAiIncubatingAttributes.GEN_AI_CONVERSATION_ID.getKey()));
        }

    }

}
