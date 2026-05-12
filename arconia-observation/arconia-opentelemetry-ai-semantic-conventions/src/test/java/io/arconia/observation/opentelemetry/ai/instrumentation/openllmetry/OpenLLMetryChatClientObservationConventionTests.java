package io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry;

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
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLLMetryChatClientObservationConvention}.
 */
class OpenLLMetryChatClientObservationConventionTests {

    private final OpenTelemetryAiConventionsProperties properties = new OpenTelemetryAiConventionsProperties();
    private final OpenLLMetryChatClientObservationConvention convention =
            new OpenLLMetryChatClientObservationConvention(properties);

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        var context = createContext("Hello");

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "invoke_workflow"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM, "spring_ai"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_IS_STREAMING, "false"),
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_NAME, "chat_client"),
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, OpenLLMetryAttributes.TraceloopSpanKind.WORKFLOW.getValue())
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
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_IS_STREAMING, "true")
        );
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
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_CONVERSATION_ID.getKey(), "session-42")
        );
    }

    @Test
    void shouldNotIncludeConversationIdWhenAbsent() {
        var context = createContext("Hello");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(GenAiIncubatingAttributes.GEN_AI_CONVERSATION_ID.getKey()));
    }

    @Test
    void shouldNotIncludeContentWhenCaptureContentIsNone() {
        var context = createContext("Hello");
        setResponse(context, "Hi there!", "stop");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT));
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT));
    }

    @Test
    void shouldIncludeContentWhenCaptureContentIsSpanAttributes() throws JSONException {
        properties.setCaptureContent(OpenTelemetryAiConventionsProperties.CaptureContentFormat.SPAN_ATTRIBUTES);
        var context = createContextWithMessages(
                List.of(new SystemMessage("Be helpful"), new UserMessage("Hi")));
        setResponse(context, "Hello!", "stop");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        String inputJson = findKeyValue(keyValues, OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT);
        JSONAssert.assertEquals("""
                [
                  {"role": "system", "parts": [{"type": "text", "content": "Be helpful"}]},
                  {"role": "user", "parts": [{"type": "text", "content": "Hi"}]}
                ]""", inputJson, JSONCompareMode.STRICT);

        String outputJson = findKeyValue(keyValues, OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT);
        JSONAssert.assertEquals("""
                [
                  {"role": "assistant", "parts": [{"type": "text", "content": "Hello!"}], "finish_reason": "stop"}
                ]""", outputJson, JSONCompareMode.STRICT);
    }

    @Test
    void shouldNotIncludeContentWhenCaptureContentIsSpanEvents() {
        properties.setCaptureContent(OpenTelemetryAiConventionsProperties.CaptureContentFormat.SPAN_EVENTS);
        var context = createContext("Hello");
        setResponse(context, "Hi there!", "stop");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT));
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT));
    }

    @Test
    void shouldIncludeInputContentButNotOutputWhenNoResponse() throws JSONException {
        properties.setCaptureContent(OpenTelemetryAiConventionsProperties.CaptureContentFormat.SPAN_ATTRIBUTES);
        var context = createContextWithMessages(List.of(new UserMessage("Hi")));

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        String inputJson = findKeyValue(keyValues, OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT);
        JSONAssert.assertEquals("""
                [
                  {"role": "user", "parts": [{"type": "text", "content": "Hi"}]}
                ]""", inputJson, JSONCompareMode.STRICT);

        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT));
    }

    // Helpers

    private ChatClientObservationContext createContext(String content) {
        return ChatClientObservationContext.builder()
                .request(ChatClientRequest.builder()
                        .prompt(Prompt.builder().content(content).build())
                        .build())
                .build();
    }

    private ChatClientObservationContext createContextWithMessages(List<org.springframework.ai.chat.messages.Message> messages) {
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
                        org.springframework.ai.chat.metadata.ChatGenerationMetadata.builder()
                                .finishReason(finishReason).build())))
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
