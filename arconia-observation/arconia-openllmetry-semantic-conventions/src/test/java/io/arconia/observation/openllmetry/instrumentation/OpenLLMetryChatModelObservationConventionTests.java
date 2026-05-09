package io.arconia.observation.openllmetry.instrumentation;

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
import org.springframework.ai.chat.messages.ToolResponseMessage;
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
import org.springframework.ai.observation.conventions.AiObservationAttributes;
import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLLMetryChatModelObservationConvention}.
 */
class OpenLLMetryChatModelObservationConventionTests {

    private final OpenLLMetryOptions openLLMetryOptions = new OpenLLMetryOptions();
    private final OpenLLMetryChatModelObservationConvention convention
            = new OpenLLMetryChatModelObservationConvention(openLLMetryOptions);

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
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM, "spring_ai"),
                KeyValue.of(AiObservationAttributes.AI_OPERATION_TYPE.value(), "chat"),
                KeyValue.of(AiObservationAttributes.REQUEST_MODEL.value(), "mistral")
        );
        assertThat(convention.getLowCardinalityKeyValues(context)).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND));
    }

    @Test
    void shouldHaveLowCardinalityKeyValuesWithResponse() {
        ChatModelObservationContext context = createContext("mistral");
        setResponse(context, "mistral-42", "Hello", "stop");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM, "spring_ai"),
                KeyValue.of(AiObservationAttributes.AI_OPERATION_TYPE.value(), "chat"),
                KeyValue.of(AiObservationAttributes.REQUEST_MODEL.value(), "mistral"),
                KeyValue.of(AiObservationAttributes.RESPONSE_MODEL.value(), "mistral-42")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithRequestParams() {
        ChatOptions options = ChatOptions.builder()
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
                .prompt(new Prompt(List.of(new UserMessage("Hi")), options))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(
                KeyValue.of(AiObservationAttributes.REQUEST_MAX_TOKENS.value(), "1000"),
                KeyValue.of(AiObservationAttributes.REQUEST_TEMPERATURE.value(), "0.7"),
                KeyValue.of(AiObservationAttributes.REQUEST_TOP_P.value(), "0.9"),
                KeyValue.of(AiObservationAttributes.REQUEST_TOP_K.value(), "40"),
                KeyValue.of(AiObservationAttributes.REQUEST_FREQUENCY_PENALTY.value(), "0.5"),
                KeyValue.of(AiObservationAttributes.REQUEST_PRESENCE_PENALTY.value(), "0.3")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithResponse() throws JSONException {
        ChatModelObservationContext context = createContextWithMessages(
                List.of(
                        new SystemMessage("You are a helpful assistant"),
                        new UserMessage("Tell me about Spring AI")
                ),
                "mistral"
        );
        setResponse(context, "mistral-42", "Spring AI is a framework...", "stop");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(
                KeyValue.of(AiObservationAttributes.RESPONSE_ID.value(), "resp-1"),
                KeyValue.of(AiObservationAttributes.USAGE_INPUT_TOKENS.value(), "1000"),
                KeyValue.of(AiObservationAttributes.USAGE_OUTPUT_TOKENS.value(), "500"),
                KeyValue.of(AiObservationAttributes.USAGE_TOTAL_TOKENS.value(), "1500"),
                KeyValue.of(AiObservationAttributes.USAGE_CACHE_WRITE_INPUT_TOKENS.value(), "1000"),
                KeyValue.of(AiObservationAttributes.USAGE_CACHE_READ_INPUT_TOKENS.value(), "500")
        );

        String inputMessages = findKeyValue(keyValues, OpenLLMetryAttributes.GEN_AI_INPUT_MESSAGES);
        JSONAssert.assertEquals("""
                [
                    {"role":"system","parts":[{"type":"text","content":"You are a helpful assistant"}]},
                    {"role":"user","parts":[{"type":"text","content":"Tell me about Spring AI"}]}
                ]""", inputMessages, JSONCompareMode.LENIENT);

        String outputMessages = findKeyValue(keyValues, OpenLLMetryAttributes.GEN_AI_OUTPUT_MESSAGES);
        JSONAssert.assertEquals("""
                [{"role":"assistant","parts":[{"type":"text","content":"Spring AI is a framework..."}],"finish_reason":"stop"}]""",
                outputMessages, JSONCompareMode.LENIENT);
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithToolCalls() throws JSONException {
        List<Message> messages = List.of(
                new UserMessage("What's the weather in New York?")
        );

        ToolDefinition weatherTool = ToolDefinition.builder()
                .name("get_weather")
                .description("Get the current weather in a location")
                .inputSchema("""
                        {
                            "type": "object",
                            "properties": {
                                "location": {
                                    "type": "string"
                                }
                            },
                            "required": ["location"]
                        }
                        """)
                .build();

        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .model("mistral")
                .toolCallbacks(new TestToolCallback(weatherTool))
                .toolNames("search_web")
                .build();

        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(messages, options))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        AssistantMessage assistantMessage = AssistantMessage.builder()
                .content("I'll check the weather for you")
                .toolCalls(List.of(new AssistantMessage.ToolCall("call_1", "function", "get_weather", "{\"location\":\"New York\"}")))
                .build();

        ChatResponse response = ChatResponse.builder()
                .metadata(ChatResponseMetadata.builder().model("mistral-42").usage(new TestUsage()).build())
                .generations(List.of(new Generation(assistantMessage)))
                .build();
        context.setResponse(response);

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        String tools = findKeyValue(keyValues, OpenLLMetryAttributes.GEN_AI_TOOL_DEFINITIONS);
        JSONAssert.assertEquals("""
                [
                    {"type":"function","name":"get_weather","description":"Get the current weather in a location","parameters":{"type":"object","properties":{"location":{"type":"string"}},"required":["location"]}},
                    {"type":"function","name":"search_web"}
                ]""", tools, JSONCompareMode.LENIENT);

        String outputMessages = findKeyValue(keyValues, OpenLLMetryAttributes.GEN_AI_OUTPUT_MESSAGES);
        JSONAssert.assertEquals("""
                [{"role":"assistant","parts":[
                    {"type":"text","content":"I'll check the weather for you"},
                    {"type":"tool_call","id":"call_1","name":"get_weather","arguments":{"location":"New York"}}
                ]}]""",
                outputMessages, JSONCompareMode.LENIENT);
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithToolResponseMessage() throws JSONException {
        List<Message> messages = List.of(
                new UserMessage("What's the weather in Rome?"),
                ToolResponseMessage.builder().responses(List.of(new ToolResponseMessage.ToolResponse(
                        "call_123",
                        "weather_tool",
                        "Sunny and warm"
                ))).build()
        );
        ChatOptions options = ChatOptions.builder().model("mistral").build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(messages, options))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        String inputMessages = findKeyValue(keyValues, OpenLLMetryAttributes.GEN_AI_INPUT_MESSAGES);
        JSONAssert.assertEquals("""
                [
                    {"role":"user","parts":[{"type":"text","content":"What's the weather in Rome?"}]},
                    {"role":"tool","parts":[{"type":"tool_call_response","id":"call_123","response":"Sunny and warm"}]}
                ]""", inputMessages, JSONCompareMode.LENIENT);
    }

    @Test
    void shouldNotIncludeContentWhenIncludeContentIsDisabled() {
        ChatModelObservationContext context = createContextWithMessages(
                List.of(
                        new SystemMessage("You are a helpful assistant"),
                        new UserMessage("Tell me about Spring AI")
                ),
                "mistral"
        );
        setResponse(context, "mistral-42", "Spring AI is a framework...", "stop");

        OpenLLMetryOptions noContentOptions = new OpenLLMetryOptions();
        noContentOptions.getInference().setIncludeContent(false);
        var noContentConvention = new OpenLLMetryChatModelObservationConvention(noContentOptions);

        KeyValues keyValues = noContentConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues.stream().map(KeyValue::getKey)).doesNotContain(
                OpenLLMetryAttributes.GEN_AI_INPUT_MESSAGES,
                OpenLLMetryAttributes.GEN_AI_OUTPUT_MESSAGES
        );
    }

    @Test
    void shouldNotIncludeToolDefinitionsWhenDisabled() {
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .model("mistral")
                .toolNames("get_weather")
                .build();

        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("Weather?")), options))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        OpenLLMetryOptions noToolsOptions = new OpenLLMetryOptions();
        noToolsOptions.getInference().setIncludeToolDefinitions(false);
        var noToolsConvention = new OpenLLMetryChatModelObservationConvention(noToolsOptions);

        KeyValues keyValues = noToolsConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(OpenLLMetryAttributes.GEN_AI_TOOL_DEFINITIONS));
    }

    @Test
    void shouldHaveOutputTypeJsonWhenStructuredOutputOptionsWithSchema() {
        TestStructuredOutputChatOptions chatOptions = new TestStructuredOutputChatOptions(
                """
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
    void shouldNotHaveOutputTypeWhenNotStructuredOutputOptions() {
        ChatModelObservationContext context = createContext("mistral");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(GenAiIncubatingAttributes.GEN_AI_OUTPUT_TYPE.getKey()));
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
