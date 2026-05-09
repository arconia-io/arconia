package io.arconia.observation.openllmetry.instrumentation;

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
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLLMetryChatClientObservationConvention}.
 */
class OpenLLMetryChatClientObservationConventionTests {

    private final OpenLLMetryOptions openLLMetryOptions = new OpenLLMetryOptions();
    private final OpenLLMetryChatClientObservationConvention convention
            = new OpenLLMetryChatClientObservationConvention(openLLMetryOptions);

    @Test
    void shouldHaveName() {
        assertThat(convention.getName())
                .isEqualTo(OpenLLMetryChatClientObservationConvention.DEFAULT_NAME);
    }

    @Test
    void contextualName() {
        var context = createContext("Hello");

        assertThat(convention.getContextualName(context)).isEqualTo("spring_ai chat_client");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        var context = createContext("Hello");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, "workflow"),
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_NAME, "chat_client")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithConversationId() throws JSONException {
        var context = ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content("Hello").build())
                        .context(ChatMemory.CONVERSATION_ID, "007")
                        .build())
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_CONVERSATION_ID.getKey(), "007")
        );
        String inputJson = findKeyValue(keyValues, OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT);
        JSONAssert.assertEquals("""
                [{"role":"user","parts":[{"type":"text","content":"Hello"}]}]""",
                inputJson, JSONCompareMode.STRICT);
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithMessages() throws JSONException {
        var context = createContextWithMessages(
                List.of(new SystemMessage("Be helpful"), new UserMessage("Hi")));
        setResponse(context, "Hello!", "stop");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        String inputJson = findKeyValue(keyValues, OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT);
        JSONAssert.assertEquals("""
                [
                  {"role":"system","parts":[{"type":"text","content":"Be helpful"}]},
                  {"role":"user","parts":[{"type":"text","content":"Hi"}]}
                ]""", inputJson, JSONCompareMode.STRICT);

        String outputJson = findKeyValue(keyValues, OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT);
        JSONAssert.assertEquals("""
                [{"role":"assistant","parts":[{"type":"text","content":"Hello!"}],"finish_reason":"stop"}]""",
                outputJson, JSONCompareMode.STRICT);
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithToolResponseMessage() throws JSONException {
        var context = createContextWithMessages(List.of(
                new UserMessage("What's the weather in Rome?"),
                ToolResponseMessage.builder().responses(List.of(new ToolResponseMessage.ToolResponse(
                        "call_123", "weather_tool", "Sunny and warm"
                ))).build()
        ));

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        String inputJson = findKeyValue(keyValues, OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT);
        JSONAssert.assertEquals("""
                [
                  {"role":"user","parts":[{"type":"text","content":"What's the weather in Rome?"}]},
                  {"role":"tool","parts":[{"type":"tool_call_response","id":"call_123","response":"Sunny and warm"}]}
                ]""", inputJson, JSONCompareMode.LENIENT);
    }

    @Test
    void shouldNotIncludeOutputMessagesWhenNoResponse() {
        var context = createContextWithMessages(List.of(new UserMessage("Hi")));

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT));
    }

    @Test
    void shouldNotIncludeContentWhenDisabled() {
        var context = createContext("Hello");
        setResponse(context, "Hi there!", "stop");

        OpenLLMetryOptions noContentOptions = new OpenLLMetryOptions();
        noContentOptions.getInference().setIncludeContent(false);
        var noContentConvention = new OpenLLMetryChatClientObservationConvention(noContentOptions);

        KeyValues keyValues = noContentConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues.stream().map(KeyValue::getKey)).doesNotContain(
                OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT,
                OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT
        );
    }

    // Helpers

    private ChatClientObservationContext createContext(String content) {
        return ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content(content).build())
                        .build())
                .build();
    }

    private ChatClientObservationContext createContextWithMessages(List<Message> messages) {
        return ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(new Prompt(messages))
                        .build())
                .build();
    }

    private void setResponse(ChatClientObservationContext context, String content, String finishReason) {
        var chatResponse = ChatResponse.builder()
                .generations(List.of(new Generation(
                        AssistantMessage.builder().content(content).build(),
                        ChatGenerationMetadata.builder().finishReason(finishReason).build())))
                .build();
        context.setResponse(ChatClientResponse.builder().chatResponse(chatResponse).build());
    }

    private String findKeyValue(KeyValues keyValues, String key) {
        return keyValues.stream()
                .filter(kv -> Objects.equals(kv.getKey(), key))
                .findFirst()
                .map(KeyValue::getValue)
                .orElseThrow(() -> new AssertionError("Key not found: " + key));
    }

}
