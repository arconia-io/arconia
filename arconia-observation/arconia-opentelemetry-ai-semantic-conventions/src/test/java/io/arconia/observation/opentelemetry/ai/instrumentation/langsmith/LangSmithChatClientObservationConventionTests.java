package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LangSmithChatClientObservationConvention}.
 */
class LangSmithChatClientObservationConventionTests {

    private final OpenTelemetryAiConventionsProperties properties = new OpenTelemetryAiConventionsProperties();
    private final LangSmithChatClientObservationConvention convention =
            new LangSmithChatClientObservationConvention(properties);

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        var context = createContext("Hello");

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "invoke_workflow"),
                KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM.getKey(), "spring_ai"),
                KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(), "chain")
        );
    }

    @Test
    void shouldIncludeStreamFalseWhenNotStreaming() {
        var context = createContext("Hello");

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_STREAM.getKey(), "false")
        );
    }

    @Test
    void shouldIncludeStreamTrueWhenStreaming() {
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .build())
                .stream(true)
                .build();

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_STREAM.getKey(), "true")
        );
    }

    @Test
    void shouldNotUseOtelConversationIdKeyWhenConversationIdPresent() {
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .context(ChatMemory.CONVERSATION_ID, "session-42")
                        .build())
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(GenAiIncubatingAttributes.GEN_AI_CONVERSATION_ID.getKey()));
    }

    @Test
    void shouldNotIncludeContentEvenWhenCaptureContentIsSpanAttributes() {
        properties.setCaptureContent(OpenTelemetryAiConventionsProperties.CaptureContentFormat.SPAN_ATTRIBUTES);
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(new Prompt(java.util.List.of(new UserMessage("Hi"))))
                        .build())
                .build();
        var chatResponse = ChatResponse.builder()
                .generations(java.util.List.of(new Generation(
                        AssistantMessage.builder().content("Hello!").build(),
                        ChatGenerationMetadata.builder().finishReason("stop").build())))
                .build();
        context.setResponse(ChatClientResponse.builder().chatResponse(chatResponse).build());

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(LangSmithAttributes.GEN_AI_PROMPT.getKey()));
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(LangSmithAttributes.GEN_AI_COMPLETION.getKey()));
    }

    @Test
    void shouldIncludeConversationIdWhenPresent() {
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .context(ChatMemory.CONVERSATION_ID, "session-42")
                        .build())
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(LangSmithAttributes.LANGSMITH_TRACE_SESSION_ID.getKey(), "session-42")
        );
    }

    @Test
    void shouldNotIncludeConversationIdWhenAbsent() {
        var context = createContext("Hello");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(LangSmithAttributes.LANGSMITH_TRACE_SESSION_ID.getKey()));
    }

    // Helpers

    private ChatClientObservationContext createContext(String content) {
        return ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content(content).build())
                        .build())
                .build();
    }

}
