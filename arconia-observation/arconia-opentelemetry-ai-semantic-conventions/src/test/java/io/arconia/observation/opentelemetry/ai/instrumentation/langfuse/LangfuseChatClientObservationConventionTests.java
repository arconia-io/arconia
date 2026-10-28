package io.arconia.observation.opentelemetry.ai.instrumentation.langfuse;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties.InferenceContentDestination;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LangfuseChatClientObservationConvention}.
 */
class LangfuseChatClientObservationConventionTests {

    private final OpenTelemetryAiConventionsProperties properties = new OpenTelemetryAiConventionsProperties();

    private final LangfuseChatClientObservationConvention convention = new LangfuseChatClientObservationConvention(properties);

    @Test
    void shouldRecordContentOnSpanAttributesWhateverTheDestination() {
        properties.setCaptureMessageContent(true);
        properties.setInferenceContentDestination(InferenceContentDestination.LOG_EVENTS);
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(new Prompt(List.of(new UserMessage("Hello"))))
                        .build())
                .build();
        var chatResponse = ChatResponse.builder()
                .generations(List.of(new Generation(AssistantMessage.builder().content("Hi!").build())))
                .build();
        context.setResponse(ChatClientResponse.builder().chatResponse(chatResponse).build());

        assertThat(convention.getHighCardinalityKeyValues(context))
                .anySatisfy(keyValue -> assertThat(keyValue.getKey()).isEqualTo(GenAiAttributes.GEN_AI_INPUT_MESSAGES.getKey()))
                .anySatisfy(keyValue -> assertThat(keyValue.getKey()).isEqualTo(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES.getKey()));
    }

}
