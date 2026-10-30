package io.arconia.observation.opentelemetry.ai.autoconfigure;

import java.util.List;

import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.observation.opentelemetry.ai.instrumentation.langfuse.LangfuseAdvisorObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langfuse.LangfuseChatClientObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.langfuse.LangfuseChatModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConversationIdObservationHandler;

/**
 * Conventions of the {@code langfuse} flavor, on top of the shared OpenTelemetry GenAI ones.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = OpenTelemetryAiConventionsProperties.CONFIG_PREFIX, name = "flavor", havingValue = "langfuse")
class LangfuseFlavorConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatModelObservationConvention.class)
    LangfuseChatModelObservationConvention chatModelObservationConvention(OpenTelemetryAiConventionsProperties properties) {
        return new LangfuseChatModelObservationConvention(properties);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ ChatClientObservationConvention.class, AdvisorObservationConvention.class })
    static class ChatClientConventionsConfiguration {

        @Bean
        @ConditionalOnMissingBean(ChatClientObservationConvention.class)
        LangfuseChatClientObservationConvention chatClientObservationConvention(OpenTelemetryAiConventionsProperties properties) {
            return new LangfuseChatClientObservationConvention(properties);
        }

        @Bean
        @ConditionalOnMissingBean(AdvisorObservationConvention.class)
        LangfuseAdvisorObservationConvention advisorObservationConvention() {
            return new LangfuseAdvisorObservationConvention();
        }

        @Bean
        @ConditionalOnMissingBean(GenAiConversationIdObservationHandler.class)
        GenAiConversationIdObservationHandler conversationIdObservationHandler() {
            return new GenAiConversationIdObservationHandler(List.of(GenAiIncubatingAttributes.GEN_AI_CONVERSATION_ID.getKey()));
        }

    }

}
