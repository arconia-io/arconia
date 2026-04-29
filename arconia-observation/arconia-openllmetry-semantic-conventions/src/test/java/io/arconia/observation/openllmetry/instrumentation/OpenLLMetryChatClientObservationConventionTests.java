package io.arconia.observation.openllmetry.instrumentation;

import java.util.List;

import io.micrometer.common.KeyValue;

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
 * Unit tests for {@link OpenLLMetryChatClientObservationConvention}.
 */
class OpenLLMetryChatClientObservationConventionTests {

    private final OpenLLMetryOptions openLLMetryOptions = new OpenLLMetryOptions();
    private final OpenLLMetryChatClientObservationConvention observationConvention
            = new OpenLLMetryChatClientObservationConvention(openLLMetryOptions);

    @Test
    void name() {
        assertThat(observationConvention.getName())
                .isEqualTo(OpenLLMetryChatClientObservationConvention.DEFAULT_NAME);
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
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, "workflow")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValues() {
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .context(ChatMemory.CONVERSATION_ID, "007")
                        .build())
                .build();

        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ASSOCIATION_PROPERTIES + ".conversation_id", "007"),
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, "Hello")
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
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, "Hello"),
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, "Hi there!")
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
                .noneSatisfy(kv -> assertThat(kv.getKey()).isEqualTo(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT));
    }

    @Test
    void shouldRedactContentWhenTraceContentDisabled() {
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .build())
                .build();
        var chatResponse = ChatResponse.builder()
                .generations(List.of(new Generation(new AssistantMessage("Hi there!"))))
                .build();
        context.setResponse(ChatClientResponse.builder().chatResponse(chatResponse).build());

        OpenLLMetryOptions redactingOptions = new OpenLLMetryOptions();
        redactingOptions.setTraceContent(false);
        var redactingConvention = new OpenLLMetryChatClientObservationConvention(redactingOptions);

        assertThat(redactingConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, OpenLLMetryOptions.REDACTED_PLACEHOLDER),
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, OpenLLMetryOptions.REDACTED_PLACEHOLDER)
        );
    }

}
