package io.arconia.observation.opentelemetry.instrumentation.genai;

import java.util.List;
import java.util.Objects;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.observation.ChatClientObservationContext;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryChatClientObservationConvention}.
 */
class OpenTelemetryChatClientObservationConventionTests {

    private final OpenTelemetryGenAiOptions options = new OpenTelemetryGenAiOptions();
    private final OpenTelemetryChatClientObservationConvention convention =
            new OpenTelemetryChatClientObservationConvention(options);

    @Test
    void shouldNotIncludeContentWhenCaptureContentIsNone() {
        var context = createContext("Hello");
        setResponse(context, "Hi there!", "stop");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith("gen_ai.input.messages"));
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith("gen_ai.output.messages"));
    }

    @Test
    void shouldIncludeContentWhenCaptureContentIsSpanAttributes() throws JSONException {
        options.getInference().setCaptureContent(OpenTelemetryGenAiOptions.CaptureContentFormat.SPAN_ATTRIBUTES);
        var context = createContextWithMessages(
                List.of(new SystemMessage("Be helpful"), new UserMessage("Hi")));
        setResponse(context, "Hello!", "stop");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        String inputJson = findKeyValue(keyValues, GenAiMoreIncubatingAttributes.GEN_AI_INPUT_MESSAGES.getKey());
        JSONAssert.assertEquals("""
                [
                  {"role": "system", "parts": [{"type": "text", "content": "Be helpful"}]},
                  {"role": "user", "parts": [{"type": "text", "content": "Hi"}]}
                ]""", inputJson, JSONCompareMode.STRICT);

        String outputJson = findKeyValue(keyValues, GenAiMoreIncubatingAttributes.GEN_AI_OUTPUT_MESSAGES.getKey());
        JSONAssert.assertEquals("""
                [
                  {"role": "assistant", "parts": [{"type": "text", "content": "Hello!"}], "finish_reason": "stop"}
                ]""", outputJson, JSONCompareMode.STRICT);
    }

    @Test
    void shouldNotIncludeContentWhenCaptureContentIsSpanEvents() {
        options.getInference().setCaptureContent(OpenTelemetryGenAiOptions.CaptureContentFormat.SPAN_EVENTS);
        var context = createContext("Hello");
        setResponse(context, "Hi there!", "stop");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith("gen_ai.input.messages"));
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith("gen_ai.output.messages"));
    }

    @Test
    void shouldIncludeInputContentButNotOutputWhenNoResponse() throws JSONException {
        options.getInference().setCaptureContent(OpenTelemetryGenAiOptions.CaptureContentFormat.SPAN_ATTRIBUTES);
        var context = createContextWithMessages(List.of(new UserMessage("Hi")));

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        String inputJson = findKeyValue(keyValues, GenAiMoreIncubatingAttributes.GEN_AI_INPUT_MESSAGES.getKey());
        JSONAssert.assertEquals("""
                [
                  {"role": "user", "parts": [{"type": "text", "content": "Hi"}]}
                ]""", inputJson, JSONCompareMode.STRICT);

        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith("gen_ai.output.messages"));
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
