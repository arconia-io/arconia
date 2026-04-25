package io.arconia.observation.openinference.instrumentation;

import java.util.List;

import com.arize.semconv.trace.SemanticConventions;

import io.micrometer.common.KeyValue;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenInferenceChatClientObservationConvention}.
 */
class OpenInferenceChatClientObservationConventionTests {

    private final OpenInferenceOptions openInferenceOptions = new OpenInferenceOptions();
    private final OpenInferenceChatClientObservationConvention observationConvention
            = new OpenInferenceChatClientObservationConvention(openInferenceOptions);

    @Test
    void name() {
        assertThat(observationConvention.getName())
                .isEqualTo(OpenInferenceChatClientObservationConvention.DEFAULT_NAME);
    }

    @Test
    void contextualName() {
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .build())
                .build();
        assertThat(observationConvention.getContextualName(context)).isEqualTo("spring_ai chat_client");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .build())
                .build();

        assertThat(observationConvention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, "AGENT")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValues() throws JSONException {
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .context(ChatMemory.CONVERSATION_ID, "007")
                        .build())
                .build();

        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.SESSION_ID, "007"),
                KeyValue.of(SemanticConventions.INPUT_VALUE, "Hello")
        );
    }

    @Test
    void shouldHaveOutputValueWhenResponseAvailable() {
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .build())
                .build();
        var chatResponse = ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage("Hi there!"))))
                .build();
        context.setResponse(ChatClientResponse.builder().chatResponse(chatResponse).build());

        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.INPUT_VALUE, "Hello"),
                KeyValue.of(SemanticConventions.OUTPUT_VALUE, "Hi there!")
        );
    }

    @Test
    void shouldNotHaveOutputValueWhenNoResponse() {
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .build())
                .build();

        assertThat(observationConvention.getHighCardinalityKeyValues(context))
                .noneSatisfy(kv -> assertThat(kv.getKey()).isEqualTo(SemanticConventions.OUTPUT_VALUE));
    }

    @Test
    void shouldRedactOutputValueWhenConfigured() {
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .build())
                .build();
        var chatResponse = ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage("Hi there!"))))
                .build();
        context.setResponse(ChatClientResponse.builder().chatResponse(chatResponse).build());

        OpenInferenceOptions redactingOptions = new OpenInferenceOptions();
        redactingOptions.setHideOutputs(true);
        var redactingConvention = new OpenInferenceChatClientObservationConvention(redactingOptions);

        assertThat(redactingConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.OUTPUT_VALUE, OpenInferenceOptions.REDACTED_PLACEHOLDER)
        );
    }

}
