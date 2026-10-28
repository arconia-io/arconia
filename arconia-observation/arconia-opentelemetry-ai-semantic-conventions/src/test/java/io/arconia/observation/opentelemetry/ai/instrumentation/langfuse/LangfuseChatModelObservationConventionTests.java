package io.arconia.observation.opentelemetry.ai.instrumentation.langfuse;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.observation.conventions.AiProvider;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties.InferenceContentDestination;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LangfuseChatModelObservationConvention}.
 */
class LangfuseChatModelObservationConventionTests {

    private final OpenTelemetryAiConventionsProperties properties = new OpenTelemetryAiConventionsProperties();

    private final LangfuseChatModelObservationConvention convention = new LangfuseChatModelObservationConvention(properties);

    @Test
    void shouldRecordContentOnSpanAttributesWhateverTheDestination() {
        properties.setCaptureMessageContent(true);
        properties.setInferenceContentDestination(InferenceContentDestination.LOG_EVENTS);
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("Hello")), ChatOptions.builder().model("mistral").build()))
                .provider(AiProvider.SPRING_AI.value())
                .build();
        context.setResponse(ChatResponse.builder()
                .generations(List.of(new Generation(AssistantMessage.builder().content("Hi!").build())))
                .build());

        assertThat(convention.getHighCardinalityKeyValues(context))
                .anySatisfy(keyValue -> assertThat(keyValue.getKey()).isEqualTo(GenAiAttributes.GEN_AI_INPUT_MESSAGES.getKey()))
                .anySatisfy(keyValue -> assertThat(keyValue.getKey()).isEqualTo(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES.getKey()));
    }

}
