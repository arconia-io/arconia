package io.arconia.openinference.observation.instrumentation.ai;

import com.arize.semconv.trace.SemanticConventions;

import io.micrometer.common.KeyValue;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenInferenceChatClientObservationConvention}.
 */
class OpenInferenceChatClientObservationConventionTests {

    private final OpenInferenceTracingOptions tracingOptions = new OpenInferenceTracingOptions();
    private final OpenInferenceChatClientObservationConvention observationConvention
            = new OpenInferenceChatClientObservationConvention(tracingOptions);

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
                KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, "CHAIN")
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

}
