package io.arconia.observation.openllmetry.instrumentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLLMetryChatModelObservationConvention}.
 */
class OpenLLMetryChatModelObservationConventionTests {

    private final OpenLLMetryOptions openLLMetryOptions = new OpenLLMetryOptions();
    private final OpenLLMetryChatModelObservationConvention observationConvention
            = new OpenLLMetryChatModelObservationConvention(openLLMetryOptions);

    @Test
    void shouldHaveName() {
        assertThat(observationConvention.getName())
            .isEqualTo(OpenLLMetryChatModelObservationConvention.DEFAULT_NAME);
    }

    @Test
    void contextualNameWhenModelIsDefined() {
        ChatOptions options = ChatOptions.builder().model("mistral").build();
        Prompt prompt = Prompt.builder().content("Hey").chatOptions(options).build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(prompt)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        assertThat(observationConvention.getContextualName(context)).isEqualTo("chat mistral");
    }

    @Test
    void contextualNameWhenModelIsNotDefined() {
        Prompt prompt = Prompt.builder().content("Hey").build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(prompt)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        assertThat(observationConvention.getContextualName(context)).isEqualTo("chat");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        ChatOptions options = ChatOptions.builder().model("mistral").build();
        Prompt prompt = Prompt.builder().content("Hey").chatOptions(options).build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(prompt)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        assertThat(observationConvention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, "task"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM, "spring_ai"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_OPERATION_NAME, "chat"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_MODEL, "mistral")
        );
    }

    @Test
    void shouldHaveLowCardinalityKeyValuesWithResponse() {
        ChatOptions options = ChatOptions.builder().model("mistral").build();
        Prompt prompt = Prompt.builder().content("Hey").chatOptions(options).build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(prompt)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        ChatResponse response = ChatResponse.builder()
                .metadata(ChatResponseMetadata.builder().model("mistral-42").usage(new TestUsage()).build())
                .generations(List.of(new Generation(AssistantMessage.builder().content("Hello").build())))
                .build();
        context.setResponse(response);

        assertThat(observationConvention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, "task"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM, "spring_ai"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_OPERATION_NAME, "chat"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_MODEL, "mistral-42")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithRequest() throws JSONException {
        List<Message> messages = List.of(
                new SystemMessage("You are a helpful assistant"),
                new UserMessage("Tell me about Spring AI")
        );
        ChatOptions options = ChatOptions.builder()
                .model("mistral")
                .maxTokens(1000)
                .temperature(0.7)
                .topP(0.9)
                .build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(messages, options))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(
                KeyValue.of(OpenLLMetryAttributes.LLM_REQUEST_TYPE, "chat"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_TEMPERATURE, "0.7"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_MAX_TOKENS, "1000"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_TOP_P, "0.9")
        );

        String entityInput = findKeyValue(keyValues, OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT);
        JSONAssert.assertEquals("""
                [
                    {"role":"system","content":"You are a helpful assistant"},
                    {"role":"user","content":"Tell me about Spring AI"}
                ]""", entityInput, JSONCompareMode.LENIENT);
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithResponse() throws JSONException {
        List<Message> messages = List.of(
                new SystemMessage("You are a helpful assistant"),
                new UserMessage("Tell me about Spring AI")
        );
        ChatOptions options = ChatOptions.builder()
                .model("mistral")
                .maxTokens(1000)
                .temperature(0.7)
                .topP(0.9)
                .build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(messages, options))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        ChatResponse response = ChatResponse.builder()
                .metadata(ChatResponseMetadata.builder().model("mistral-42").usage(new TestUsage()).build())
                .generations(List.of(new Generation(AssistantMessage.builder().content("Spring AI is a framework...").build())))
                .build();
        context.setResponse(response);

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_USAGE_INPUT_TOKENS, "1000"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_USAGE_OUTPUT_TOKENS, "500"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_USAGE_TOTAL_TOKENS, "1500")
        );

        String entityOutput = findKeyValue(keyValues, OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT);
        JSONAssert.assertEquals("""
                [{"role":"assistant","content":"Spring AI is a framework..."}]""",
                entityOutput, JSONCompareMode.LENIENT);
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

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);

        String tools = findKeyValue(keyValues, OpenLLMetryAttributes.TRACELOOP_ENTITY_NAME + ".tools");
        JSONAssert.assertEquals("""
                [
                    {"function":{"name":"get_weather","description":"Get the current weather in a location","parameters":{"type":"object","properties":{"location":{"type":"string"}},"required":["location"]}},"type":"function"},
                    {"function":{"name":"search_web"},"type":"function"}
                ]""", tools, JSONCompareMode.LENIENT);

        String entityOutput = findKeyValue(keyValues, OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT);
        JSONAssert.assertEquals("""
                [{"role":"assistant","content":"I'll check the weather for you","tool_calls":[{"id":"call_1","function":{"name":"get_weather","arguments":"{\\"location\\":\\"New York\\"}"}}]}]""",
                entityOutput, JSONCompareMode.LENIENT);
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

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);

        String entityInput = findKeyValue(keyValues, OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT);
        JSONAssert.assertEquals("""
                [
                    {"role":"user","content":"What's the weather in Rome?"},
                    {"role":"tool","tool_call_id":"call_123","name":"weather_tool","content":"Sunny and warm"}
                ]""", entityInput, JSONCompareMode.LENIENT);
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithAllRequestParameters() {
        ChatOptions options = ChatOptions.builder()
                .model("mistral")
                .maxTokens(1000)
                .temperature(0.7)
                .topP(0.9)
                .topK(40)
                .frequencyPenalty(0.5)
                .presencePenalty(0.3)
                .stopSequences(List.of("STOP", "END"))
                .build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("Hello")), options))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_TEMPERATURE, "0.7"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_MAX_TOKENS, "1000"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_TOP_P, "0.9"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_TOP_K, "40"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_FREQUENCY_PENALTY, "0.5"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_PRESENCE_PENALTY, "0.3")
        );

        assertThat(findKeyValue(keyValues, OpenLLMetryAttributes.GEN_AI_REQUEST_STOP_SEQUENCES))
                .contains("STOP").contains("END");
    }

    @Test
    void shouldRedactContentWhenTraceContentDisabled() throws JSONException {
        List<Message> messages = List.of(
                new SystemMessage("You are a helpful assistant"),
                new UserMessage("Tell me about Spring AI")
        );
        ChatOptions options = ChatOptions.builder()
                .model("mistral")
                .temperature(0.7)
                .build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(messages, options))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        ChatResponse response = ChatResponse.builder()
                .metadata(ChatResponseMetadata.builder().model("mistral-42").usage(new TestUsage()).build())
                .generations(List.of(new Generation(AssistantMessage.builder().content("Spring AI is a framework...").build())))
                .build();
        context.setResponse(response);

        OpenLLMetryOptions redactingOptions = new OpenLLMetryOptions();
        redactingOptions.setTraceContent(false);
        var redactingConvention = new OpenLLMetryChatModelObservationConvention(redactingOptions);

        KeyValues keyValues = redactingConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, OpenLLMetryOptions.REDACTED_PLACEHOLDER),
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, OpenLLMetryOptions.REDACTED_PLACEHOLDER)
        );
    }

    @Test
    void shouldNotIncludeToolsWhenDisabled() {
        List<Message> messages = List.of(
                new UserMessage("What's the weather?")
        );

        ToolDefinition weatherTool = ToolDefinition.builder()
                .name("get_weather")
                .description("Get weather")
                .inputSchema("{}")
                .build();

        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .model("mistral")
                .toolCallbacks(new TestToolCallback(weatherTool))
                .build();

        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(messages, options))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        OpenLLMetryOptions noToolsOptions = new OpenLLMetryOptions();
        noToolsOptions.setIncludeToolDefinitions(false);
        var noToolsConvention = new OpenLLMetryChatModelObservationConvention(noToolsOptions);

        KeyValues keyValues = noToolsConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues)
                .noneSatisfy(kv -> assertThat(kv.getKey()).isEqualTo(OpenLLMetryAttributes.TRACELOOP_ENTITY_NAME + ".tools"));
    }

    @Test
    void shouldHaveResponseIdWhenAvailable() {
        ChatOptions options = ChatOptions.builder().model("mistral").build();
        Prompt prompt = Prompt.builder().content("Hey").chatOptions(options).build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(prompt)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        ChatResponse response = ChatResponse.builder()
                .metadata(ChatResponseMetadata.builder()
                        .model("mistral-42")
                        .id("resp-123")
                        .usage(new TestUsage())
                        .build())
                .generations(List.of(new Generation(AssistantMessage.builder().content("Hello").build())))
                .build();
        context.setResponse(response);

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_RESPONSE_ID, "resp-123")
        );
    }

    @Test
    void shouldHaveFinishReasonWhenAvailable() {
        ChatOptions options = ChatOptions.builder().model("mistral").build();
        Prompt prompt = Prompt.builder().content("Hey").chatOptions(options).build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(prompt)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        Generation generation = new Generation(AssistantMessage.builder().content("Hello").build(),
                org.springframework.ai.chat.metadata.ChatGenerationMetadata.builder().finishReason("stop").build());
        ChatResponse response = ChatResponse.builder()
                .metadata(ChatResponseMetadata.builder().model("mistral-42").usage(new TestUsage()).build())
                .generations(List.of(generation))
                .build();
        context.setResponse(response);

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_RESPONSE_FINISH_REASONS, "stop")
        );
    }

    private String findKeyValue(KeyValues keyValues, String key) {
        return keyValues.stream()
                .filter(kv -> Objects.equals(kv.getKey(), key))
                .findFirst()
                .map(KeyValue::getValue)
                .orElseThrow(() -> new AssertionError("Key not found: " + key));
    }

    static class TestUsage implements Usage {

        @Override
        public Integer getPromptTokens() {
            return 1000;
        }

        @Override
        public Integer getCompletionTokens() {
            return 500;
        }

        @Override
        public Integer getTotalTokens() {
            return 1500;
        }

        @Override
        public Map<String, Integer> getNativeUsage() {
            Map<String, Integer> usage = new HashMap<>();
            usage.put("promptTokens", getPromptTokens());
            usage.put("completionTokens", getCompletionTokens());
            usage.put("totalTokens", getTotalTokens());
            return usage;
        }
    }

    private static final class TestToolCallback implements ToolCallback {

        private final ToolDefinition toolDefinition;

        private TestToolCallback(ToolDefinition toolDefinition) {
            this.toolDefinition = toolDefinition;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return toolDefinition;
        }

        @Override
        public String call(String toolInput) {
            return "";
        }

    }

}
