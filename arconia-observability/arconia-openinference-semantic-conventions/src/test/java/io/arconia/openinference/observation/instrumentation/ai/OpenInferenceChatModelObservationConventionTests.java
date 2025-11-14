package io.arconia.openinference.observation.instrumentation.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.arize.semconv.trace.SemanticConventions;

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
 * Unit tests for {@link OpenInferenceChatModelObservationConvention}.
 */
class OpenInferenceChatModelObservationConventionTests {

    private final OpenInferenceTracingOptions tracingOptions = new OpenInferenceTracingOptions();
    private final OpenInferenceChatModelObservationConvention observationConvention
            = new OpenInferenceChatModelObservationConvention(tracingOptions);

    @Test
    void shouldHaveName() {
        assertThat(observationConvention.getName())
            .isEqualTo(OpenInferenceChatModelObservationConvention.DEFAULT_NAME);
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
                KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, "LLM"),
                KeyValue.of(SemanticConventions.LLM_PROVIDER, "spring_ai"),
                KeyValue.of(SemanticConventions.LLM_SYSTEM, "spring_ai"),
                KeyValue.of(SemanticConventions.LLM_MODEL_NAME, "mistral")
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
                .generations(List.of(new Generation(new AssistantMessage("Hello"))))
                .build();
        context.setResponse(response);

        assertThat(observationConvention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, "LLM"),
                KeyValue.of(SemanticConventions.LLM_PROVIDER, "spring_ai"),
                KeyValue.of(SemanticConventions.LLM_SYSTEM, "spring_ai"),
                KeyValue.of(SemanticConventions.LLM_MODEL_NAME, "mistral-42")
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

        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_ROLE, "system"),
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_CONTENT, "You are a helpful assistant"),
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".1." + SemanticConventions.MESSAGE_ROLE, "user"),
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".1." + SemanticConventions.MESSAGE_CONTENT, "Tell me about Spring AI"),
                KeyValue.of(SemanticConventions.LLM_INVOCATION_PARAMETERS, """
                        {"top_p":0.9,"max_tokens":1000,"temperature":0.7}""")
        );

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);
        String invocationParameters = """
               {"top_p":0.9,"max_tokens":1000,"temperature":0.7}""";

        JSONAssert.assertEquals(invocationParameters, findKeyValue(keyValues, SemanticConventions.LLM_INVOCATION_PARAMETERS), JSONCompareMode.LENIENT);
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithResponse() {
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
                .generations(List.of(new Generation(new AssistantMessage("Spring AI is a framework..."))))
                .build();
        context.setResponse(response);

        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.LLM_OUTPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_ROLE, "assistant"),
                KeyValue.of(SemanticConventions.LLM_OUTPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_CONTENT, "Spring AI is a framework..."),
                KeyValue.of(SemanticConventions.LLM_TOKEN_COUNT_PROMPT, "1000"),
                KeyValue.of(SemanticConventions.LLM_TOKEN_COUNT_COMPLETION, "500"),
                KeyValue.of(SemanticConventions.LLM_TOKEN_COUNT_TOTAL, "1500")
        );
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

        String toolSchema0 = """
               {"function":{"name":"get_weather","description":"Get the current weather in a location","parameters":{"type":"object","properties":{"location":{"type":"string"}},"required":["location"]}},"type":"function"}""";

        String toolSchema1 = """
                {"function":{"name":"search_web"},"type":"function"}""";

        JSONAssert.assertEquals(toolSchema0, findKeyValue(keyValues, SemanticConventions.LLM_TOOLS + ".0." + SemanticConventions.TOOL_JSON_SCHEMA), JSONCompareMode.LENIENT);
        JSONAssert.assertEquals(toolSchema1, findKeyValue(keyValues, SemanticConventions.LLM_TOOLS + ".1." + SemanticConventions.TOOL_JSON_SCHEMA), JSONCompareMode.LENIENT);

        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.LLM_OUTPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_TOOL_CALLS + ".0." + SemanticConventions.TOOL_CALL_ID, "call_1"),
                KeyValue.of(SemanticConventions.LLM_OUTPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_TOOL_CALLS + ".0." + SemanticConventions.TOOL_CALL_FUNCTION_NAME, "get_weather"),
                KeyValue.of(SemanticConventions.LLM_OUTPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_TOOL_CALLS + ".0." + SemanticConventions.TOOL_CALL_FUNCTION_ARGUMENTS_JSON, "{\"location\":\"New York\"}")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithToolResponseMessage() {
        List<Message> messages = List.of(
                new UserMessage("What's the weather in Rome?"),
                ToolResponseMessage.builder().responses(List.of(new ToolResponseMessage.ToolResponse(
                        "call_123",
                        "weather_tool",
                        "sto a fa a colla"
                ))).build()
        );
        ChatOptions options = ChatOptions.builder().model("mistral").build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(messages, options))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_ROLE, "user"),
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_CONTENT, "What's the weather in Rome?"),
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".1." + SemanticConventions.MESSAGE_ROLE, "tool"),
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".1." + SemanticConventions.MESSAGE_CONTENT, "sto a fa a colla"),
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".1." + SemanticConventions.MESSAGE_TOOL_CALL_ID, "call_123")
        );
    }


    @Test
    void shouldRedactMessages() {
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
                .generations(List.of(new Generation(new AssistantMessage("Spring AI is a framework..."))))
                .build();
        context.setResponse(response);

        String inputMessageContentKey = SemanticConventions.LLM_INPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_CONTENT;
        String outputMessageContentKey = SemanticConventions.LLM_OUTPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_CONTENT;

        assertRedactionBehavior(context, OpenInferenceTracingOptions::setHideLlmInvocationParameters,
                SemanticConventions.LLM_INVOCATION_PARAMETERS);
        assertRedactionBehavior(context, OpenInferenceTracingOptions::setHideInputs,
                SemanticConventions.LLM_INPUT_MESSAGES);
        assertRedactionBehavior(context, OpenInferenceTracingOptions::setHideInputMessages,
                SemanticConventions.LLM_INPUT_MESSAGES);
        assertRedactionBehavior(context, OpenInferenceTracingOptions::setHideInputText,
                inputMessageContentKey);
        assertRedactionBehavior(context, OpenInferenceTracingOptions::setHideOutputs,
                SemanticConventions.LLM_OUTPUT_MESSAGES);
        assertRedactionBehavior(context, OpenInferenceTracingOptions::setHideOutputMessages,
                SemanticConventions.LLM_OUTPUT_MESSAGES);
        assertRedactionBehavior(context, OpenInferenceTracingOptions::setHideOutputText,
                outputMessageContentKey);
    }

    @Test
    void shouldRedactToolCalls() {
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

        String toolCallArgsKey = SemanticConventions.LLM_OUTPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_TOOL_CALLS + ".0." + SemanticConventions.TOOL_CALL_FUNCTION_ARGUMENTS_JSON;

        assertRedactionBehavior(context, OpenInferenceTracingOptions::setHideInputs, SemanticConventions.LLM_TOOLS);
        assertRedactionBehavior(context, OpenInferenceTracingOptions::setHideOutputText, toolCallArgsKey);
    }

    private String findKeyValue(KeyValues keyValues, String key) {
        return keyValues.stream()
                .filter(kv -> Objects.equals(kv.getKey(), key))
                .findFirst()
                .map(KeyValue::getValue)
                .orElseThrow(() -> new AssertionError("Key not found: " + key));
    }

    private void assertRedactionBehavior(ChatModelObservationContext context,
                                         java.util.function.BiConsumer<OpenInferenceTracingOptions, Boolean> optionSetter,
                                         String expectedKey) {
        OpenInferenceTracingOptions redactingOptions = new OpenInferenceTracingOptions();
        OpenInferenceChatModelObservationConvention redactingConvention =
                new OpenInferenceChatModelObservationConvention(redactingOptions);
        optionSetter.accept(redactingOptions, true);

        assertThat(redactingConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(expectedKey, OpenInferenceTracingOptions.REDACTED_PLACEHOLDER)
        );
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
