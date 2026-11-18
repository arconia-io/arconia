package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import java.util.List;
import java.util.Objects;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
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
import io.arconia.observation.opentelemetry.ai.instrumentation.GenAiConventionFlavor;

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
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "invoke_agent"),
                KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM, "spring_ai")
        );
    }

    @Test
    void shouldHaveSpanMarkersAsHighCardinalityKeyValues() {
        var context = createContext("Hello");

        assertThat(convention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND, "chain")
        );
        assertThat(convention.getLowCardinalityKeyValues(context)).extracting(KeyValue::getKey)
                .doesNotContain(LangSmithAttributes.LANGSMITH_SPAN_KIND);
    }

    @Test
    void shouldIncludeStreamFalseWhenNotStreaming() {
        var context = createContext("Hello");

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of("spring.ai.chat.client.stream", "false")
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
                KeyValue.of("spring.ai.chat.client.stream", "true")
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
    void shouldIncludeContentAsPromptAndCompletionWhenEnabled() throws JSONException {
        properties.setCaptureMessageContent(true);
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(new Prompt(List.of(new UserMessage("Hi"))))
                        .build())
                .build();
        var chatResponse = ChatResponse.builder()
                .generations(List.of(new Generation(
                        AssistantMessage.builder().content("Hello!").build(),
                        ChatGenerationMetadata.builder().finishReason("stop").build())))
                .build();
        context.setResponse(ChatClientResponse.builder().chatResponse(chatResponse).build());

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        JSONAssert.assertEquals("""
                {"messages": [{"role": "user", "content": "Hi"}]}""",
                findKeyValue(keyValues, LangSmithAttributes.GEN_AI_PROMPT), JSONCompareMode.STRICT);
        JSONAssert.assertEquals("""
                {"messages": [{"role": "assistant", "content": "Hello!"}]}""",
                findKeyValue(keyValues, LangSmithAttributes.GEN_AI_COMPLETION), JSONCompareMode.STRICT);
    }

    @Test
    void shouldRecordContentOnSpanAttributesWhateverTheDestination() {
        properties.setFlavor(GenAiConventionFlavor.LANGSMITH);
        properties.setCaptureMessageContent(true);
        properties.setInferenceContentDestination(OpenTelemetryAiConventionsProperties.InferenceContentDestination.LOG_EVENTS);
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(new Prompt(List.of(new UserMessage("Hi"))))
                        .build())
                .build();
        context.setResponse(ChatClientResponse.builder().chatResponse(ChatResponse.builder()
                .generations(List.of(new Generation(AssistantMessage.builder().content("Hello!").build())))
                .build()).build());

        assertThat(convention.getHighCardinalityKeyValues(context)).extracting(KeyValue::getKey)
                .contains(LangSmithAttributes.GEN_AI_PROMPT, LangSmithAttributes.GEN_AI_COMPLETION);
    }

    @Test
    void shouldNotIncludeContentWhenDisabled() {
        properties.setCaptureMessageContent(false);
        var context = createContext("Hello");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(LangSmithAttributes.GEN_AI_PROMPT));
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
                KeyValue.of(LangSmithAttributes.LANGSMITH_METADATA_THREAD_ID, "session-42")
        );
    }

    @Test
    void shouldNotIncludeConversationIdWhenAbsent() {
        var context = createContext("Hello");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(LangSmithAttributes.LANGSMITH_METADATA_THREAD_ID));
    }

    // Helpers

    private String findKeyValue(KeyValues keyValues, String key) {
        return keyValues.stream()
                .filter(kv -> Objects.equals(kv.getKey(), key))
                .findFirst()
                .map(KeyValue::getValue)
                .orElseThrow(() -> new AssertionError("Key not found: " + key));
    }

    private ChatClientObservationContext createContext(String content) {
        return ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content(content).build())
                        .build())
                .build();
    }

}
