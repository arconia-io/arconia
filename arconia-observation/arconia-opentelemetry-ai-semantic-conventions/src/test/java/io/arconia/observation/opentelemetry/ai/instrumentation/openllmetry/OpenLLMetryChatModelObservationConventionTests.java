package io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.json.JSONException;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.StructuredOutputChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLLMetryChatModelObservationConvention}.
 */
class OpenLLMetryChatModelObservationConventionTests {

    private final OpenTelemetryAiConventionsProperties properties = new OpenTelemetryAiConventionsProperties();
    private final OpenLLMetryChatModelObservationConvention convention =
            new OpenLLMetryChatModelObservationConvention(properties);

    @Test
    void shouldHaveName() {
        assertThat(convention.getName())
                .isEqualTo(OpenLLMetryChatModelObservationConvention.DEFAULT_NAME);
    }

    @Test
    void contextualNameWhenModelIsDefined() {
        ChatModelObservationContext context = createContext("mistral");
        assertThat(convention.getContextualName(context)).isEqualTo("chat mistral");
    }

    @Test
    void contextualNameWhenModelIsNotDefined() {
        Prompt prompt = Prompt.builder().content("Hey").build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(prompt)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        assertThat(convention.getContextualName(context)).isEqualTo("chat");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        ChatModelObservationContext context = createContext("mistral");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "chat"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM, "spring_ai"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL.getKey(), "mistral")
        );
    }

    @Test
    void shouldHaveLowCardinalityKeyValuesWithResponse() {
        ChatModelObservationContext context = createContext("mistral");
        setResponse(context, "mistral-42", "Hello", "stop");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_RESPONSE_MODEL.getKey(), "mistral-42")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithRequestParams() {
        ChatOptions chatOptions = ChatOptions.builder()
                .model("mistral")
                .maxTokens(1000)
                .temperature(0.7)
                .topP(0.9)
                .topK(40)
                .frequencyPenalty(0.5)
                .presencePenalty(0.3)
                .stopSequences(List.of("END"))
                .build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("Hi")), chatOptions))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_FREQUENCY_PENALTY.getKey(), "0.5"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_MAX_TOKENS.getKey(), "1000"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_PRESENCE_PENALTY.getKey(), "0.3"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_STOP_SEQUENCES.getKey(), """
                        ["END"]"""),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_TEMPERATURE.getKey(), "0.7"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_TOP_K.getKey(), "40"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_TOP_P.getKey(), "0.9")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithResponse() {
        ChatModelObservationContext context = createContext("mistral");
        setResponse(context, "mistral-42", "Spring AI is great!", "stop");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_RESPONSE_FINISH_REASONS.getKey(), """
                        ["stop"]"""),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_RESPONSE_ID.getKey(), "resp-1"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_USAGE_CACHE_CREATION_INPUT_TOKENS.getKey(), "1000"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_USAGE_CACHE_READ_INPUT_TOKENS.getKey(), "500"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS.getKey(), "1000"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_USAGE_OUTPUT_TOKENS.getKey(), "500"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_USAGE_TOTAL_TOKENS, "1500")
        );
    }

    @Test
    void shouldNotIncludeToolDefinitionsWhenDisabled() {
        properties.setIncludeToolDefinitions(false);

        ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder()
                .model("mistral")
                .toolNames("get_weather")
                .build();

        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("Weather?")), chatOptions))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(GenAiAttributes.GEN_AI_TOOL_DEFINITIONS.getKey()));
    }

    @Test
    void shouldIncludeToolDefinitionsWhenEnabled() throws JSONException {
        properties.setIncludeToolDefinitions(true);

        ToolDefinition weatherTool = ToolDefinition.builder()
                .name("get_weather")
                .description("Get weather")
                .inputSchema("""
                        {"type": "object", "properties": {"location": {"type": "string"}}}""")
                .build();

        ToolCallingChatOptions chatOptions = ToolCallingChatOptions.builder()
                .model("mistral")
                .toolCallbacks(new TestToolCallback(weatherTool))
                .build();

        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("Weather?")), chatOptions))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        String toolDefs = findKeyValue(keyValues, GenAiAttributes.GEN_AI_TOOL_DEFINITIONS.getKey());
        JSONAssert.assertEquals("""
                [{"type": "function", "name": "get_weather", "description": "Get weather",
                  "parameters": {"type": "object", "properties": {"location": {"type": "string"}}}}]""",
                toolDefs, JSONCompareMode.STRICT);
    }

    @Test
    void shouldIncludeStreamAttributeWhenStreaming() {
        ChatOptions chatOptions = ChatOptions.builder().model("mistral").build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("Hi")), chatOptions))
                .provider(AiProvider.SPRING_AI.value())
                .streaming(true)
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_IS_STREAMING, "true")
        );
    }

    @Test
    void shouldNotIncludeStreamAttributeWhenNotStreaming() {
        ChatModelObservationContext context = createContext("mistral");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(OpenLLMetryAttributes.GEN_AI_IS_STREAMING));
    }

    @Test
    void shouldNotIncludeContentWhenCaptureContentIsNone() {
        ChatModelObservationContext context = createContext("mistral");
        setResponse(context, "mistral-42", "Hello", "stop");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith(GenAiAttributes.GEN_AI_INPUT_MESSAGES.getKey()));
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES.getKey()));
    }

    @Test
    void shouldIncludeContentWhenCaptureContentIsSpanAttributes() throws JSONException {
        properties.setCaptureContent(OpenTelemetryAiConventionsProperties.CaptureContentFormat.SPAN_ATTRIBUTES);
        ChatModelObservationContext context = createContextWithMessages(
                List.of(new SystemMessage("Be helpful"), new UserMessage("Hi")),
                "mistral"
        );
        setResponse(context, "mistral-42", "Hello!", "stop");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        String inputJson = findKeyValue(keyValues, GenAiAttributes.GEN_AI_INPUT_MESSAGES.getKey());
        JSONAssert.assertEquals("""
                [
                  {"role": "system", "parts": [{"type": "text", "content": "Be helpful"}]},
                  {"role": "user", "parts": [{"type": "text", "content": "Hi"}]}
                ]""", inputJson, JSONCompareMode.STRICT);

        String outputJson = findKeyValue(keyValues, GenAiAttributes.GEN_AI_OUTPUT_MESSAGES.getKey());
        JSONAssert.assertEquals("""
                [
                  {"role": "assistant", "parts": [{"type": "text", "content": "Hello!"}], "finish_reason": "stop"}
                ]""", outputJson, JSONCompareMode.STRICT);
    }

    @Test
    void shouldNotIncludeContentWhenCaptureContentIsSpanEvents() {
        properties.setCaptureContent(OpenTelemetryAiConventionsProperties.CaptureContentFormat.SPAN_EVENTS);
        ChatModelObservationContext context = createContext("mistral");
        setResponse(context, "mistral-42", "Hello", "stop");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith(GenAiAttributes.GEN_AI_INPUT_MESSAGES.getKey()));
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith(GenAiAttributes.GEN_AI_OUTPUT_MESSAGES.getKey()));
    }

    @Test
    void shouldHaveOutputTypeJsonWhenStructuredOutputOptionsWithSchema() {
        TestStructuredOutputChatOptions chatOptions = new TestStructuredOutputChatOptions("""
                {"type": "object", "properties": {"answer": {"type": "string"}}}""");
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("Hi")), chatOptions))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OUTPUT_TYPE.getKey(),
                        GenAiIncubatingAttributes.GenAiOutputTypeIncubatingValues.JSON));
    }

    @Test
    void shouldHaveOutputTypeTextWhenStructuredOutputOptionsWithoutSchema() {
        TestStructuredOutputChatOptions chatOptions = new TestStructuredOutputChatOptions(null);
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("Hi")), chatOptions))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OUTPUT_TYPE.getKey(),
                        GenAiIncubatingAttributes.GenAiOutputTypeIncubatingValues.TEXT));
    }

    @Test
    void shouldHaveOutputTypeTextWhenNotStructuredOutputOptions() {
        ChatModelObservationContext context = createContext("mistral");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OUTPUT_TYPE.getKey(),
                        GenAiIncubatingAttributes.GenAiOutputTypeIncubatingValues.TEXT));
    }

    // Helpers

    private ChatModelObservationContext createContext(String model) {
        ChatOptions chatOptions = ChatOptions.builder().model(model).build();
        Prompt prompt = Prompt.builder().content("Hey").chatOptions(chatOptions).build();
        return ChatModelObservationContext.builder()
                .prompt(prompt)
                .provider(AiProvider.SPRING_AI.value())
                .build();
    }

    private ChatModelObservationContext createContextWithMessages(List<Message> messages, String model) {
        ChatOptions chatOptions = ChatOptions.builder().model(model).build();
        return ChatModelObservationContext.builder()
                .prompt(new Prompt(messages, chatOptions))
                .provider(AiProvider.SPRING_AI.value())
                .build();
    }

    private void setResponse(ChatModelObservationContext context, String model, String content, String finishReason) {
        ChatResponse response = ChatResponse.builder()
                .metadata(ChatResponseMetadata.builder()
                        .id("resp-1")
                        .model(model)
                        .usage(new TestUsage())
                        .build())
                .generations(List.of(new Generation(
                        AssistantMessage.builder().content(content).build(),
                        ChatGenerationMetadata.builder().finishReason(finishReason).build())))
                .build();
        context.setResponse(response);
    }

    private String findKeyValue(KeyValues keyValues, String key) {
        return keyValues.stream()
                .filter(kv -> Objects.equals(kv.getKey(), key))
                .findFirst()
                .map(KeyValue::getValue)
                .orElseThrow(() -> new AssertionError("Key not found: " + key));
    }

    static class TestStructuredOutputChatOptions implements StructuredOutputChatOptions {
        private @Nullable String outputSchema;

        TestStructuredOutputChatOptions(@Nullable String outputSchema) {
            this.outputSchema = outputSchema;
        }

        @Override
        public @Nullable String getOutputSchema() {
            return this.outputSchema;
        }

        @Override
        public void setOutputSchema(@Nullable String outputSchema) {
            this.outputSchema = outputSchema;
        }

        @Override
        public @Nullable String getModel() { return null; }

        @Override
        public @Nullable Double getFrequencyPenalty() { return null; }

        @Override
        public @Nullable Integer getMaxTokens() { return null; }

        @Override
        public @Nullable Double getPresencePenalty() { return null; }

        @Override
        public @Nullable List<String> getStopSequences() { return null; }

        @Override
        public @Nullable Double getTemperature() { return null; }

        @Override
        public @Nullable Integer getTopK() { return null; }

        @Override
        public @Nullable Double getTopP() { return null; }

        @Override
        public ChatOptions copy() {
            return new TestStructuredOutputChatOptions(this.outputSchema);
        }
    }

    static class TestUsage implements Usage {
        @Override
        public Integer getPromptTokens() { return 1000; }
        @Override
        public Integer getCompletionTokens() { return 500; }
        @Override
        public Integer getTotalTokens() { return 1500; }
        @Override
        public Long getCacheWriteInputTokens() { return 1000L; }
        @Override
        public Long getCacheReadInputTokens() { return 500L; }
        @Override
        public Map<String, Integer> getNativeUsage() {
            Map<String, Integer> usage = new HashMap<>();
            usage.put("promptTokens", 1000);
            usage.put("completionTokens", 500);
            usage.put("totalTokens", 1500);
            usage.put("cacheWriteInputTokens", 1000);
            usage.put("cacheReadInputTokens", 500);
            return usage;
        }
    }

    private static final class TestToolCallback implements ToolCallback {
        private final ToolDefinition toolDefinition;
        private TestToolCallback(ToolDefinition toolDefinition) {
            this.toolDefinition = toolDefinition;
        }
        @Override
        public ToolDefinition getToolDefinition() { return toolDefinition; }
        @Override
        public String call(String toolInput) { return ""; }
    }

}
