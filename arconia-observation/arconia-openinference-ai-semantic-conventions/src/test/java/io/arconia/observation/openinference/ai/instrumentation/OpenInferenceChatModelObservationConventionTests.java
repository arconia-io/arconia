package io.arconia.observation.openinference.ai.instrumentation;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.arize.semconv.trace.SemanticConventions;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

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
import org.springframework.ai.content.Media;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenInferenceChatModelObservationConvention}.
 */
class OpenInferenceChatModelObservationConventionTests {

    private final OpenInferenceOptions openInferenceOptions = new OpenInferenceOptions();
    private final OpenInferenceChatModelObservationConvention observationConvention
            = new OpenInferenceChatModelObservationConvention(openInferenceOptions);

    @Test
    void shouldHaveName() {
        assertThat(observationConvention.getName())
            .isEqualTo("gen_ai.client.operation");
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
    void shouldNotHaveModelNameWhenUnknown() {
        Prompt prompt = Prompt.builder().content("Hey").build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(prompt)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        assertThat(observationConvention.getLowCardinalityKeyValues(context)).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(SemanticConventions.LLM_MODEL_NAME));
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
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".1." + SemanticConventions.MESSAGE_CONTENT, "Tell me about Spring AI")
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
                .generations(List.of(new Generation(AssistantMessage.builder().content("Spring AI is a framework...").build())))
                .build();
        context.setResponse(response);

        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.LLM_OUTPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_ROLE, "assistant"),
                KeyValue.of(SemanticConventions.LLM_OUTPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_CONTENT, "Spring AI is a framework..."),
                KeyValue.of(SemanticConventions.LLM_TOKEN_COUNT_PROMPT, "1000"),
                KeyValue.of(SemanticConventions.LLM_TOKEN_COUNT_COMPLETION, "500"),
                KeyValue.of(SemanticConventions.LLM_TOKEN_COUNT_TOTAL, "1500"),
                KeyValue.of(SemanticConventions.LLM_REQUEST_MODEL_NAME, "mistral"),
                KeyValue.of(SemanticConventions.LLM_RESPONSE_MODEL_NAME, "mistral-42")
        );
        // The cached input tokens are recorded as numbers by OpenInferenceChatModelObservationHandler.
        assertThat(observationConvention.getHighCardinalityKeyValues(context)).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith(SemanticConventions.LLM_TOKEN_COUNT_PROMPT_DETAILS));
    }

    @Test
    void shouldOmitTokenCountsWhenUsageIsNotReported() {
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("Hi")), ChatOptions.builder().model("mistral").build()))
                .provider(AiProvider.SPRING_AI.value())
                .build();
        context.setResponse(ChatResponse.builder()
                .metadata(ChatResponseMetadata.builder().model("mistral-42").build())
                .generations(List.of(new Generation(AssistantMessage.builder().content("Hello").build())))
                .build());

        assertThat(observationConvention.getHighCardinalityKeyValues(context)).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith("llm.token_count."));
    }

    @Test
    void shouldHaveInputAndOutputValues() throws JSONException {
        List<Message> messages = List.of(
                new SystemMessage("You are a helpful assistant"),
                new UserMessage("What's the weather in Rome?"),
                AssistantMessage.builder()
                        .content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall("call_1", "function", "weather", "{\"city\":\"Rome\"}")))
                        .build(),
                ToolResponseMessage.builder()
                        .responses(List.of(new ToolResponseMessage.ToolResponse("call_1", "weather", "sunny")))
                        .build()
        );
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(messages, ChatOptions.builder().model("mistral").build()))
                .provider(AiProvider.SPRING_AI.value())
                .build();
        context.setResponse(ChatResponse.builder()
                .metadata(ChatResponseMetadata.builder().model("mistral-42").usage(new TestUsage()).build())
                .generations(List.of(new Generation(AssistantMessage.builder().content("It's sunny").build())))
                .build());

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);
        JSONAssert.assertEquals("""
                {"messages": [
                  {"role": "system", "content": "You are a helpful assistant"},
                  {"role": "user", "content": "What's the weather in Rome?"},
                  {"role": "assistant", "content": "", "tool_calls": [
                    {"id": "call_1", "type": "function", "function": {"name": "weather", "arguments": "{\\"city\\":\\"Rome\\"}"}}
                  ]},
                  {"role": "tool", "tool_call_id": "call_1", "name": "weather", "content": "sunny"}
                ]}""", findKeyValue(keyValues, SemanticConventions.INPUT_VALUE), JSONCompareMode.STRICT);
        JSONAssert.assertEquals("""
                {"messages": [{"role": "assistant", "content": "It's sunny"}]}""",
                findKeyValue(keyValues, SemanticConventions.OUTPUT_VALUE), JSONCompareMode.STRICT);
        assertThat(keyValues).contains(
                KeyValue.of(SemanticConventions.INPUT_MIME_TYPE, "application/json"),
                KeyValue.of(SemanticConventions.OUTPUT_MIME_TYPE, "application/json"));
    }

    @Test
    void shouldRedactInputAndOutputValues() {
        OpenInferenceOptions options = new OpenInferenceOptions();
        options.setHideInputs(true);
        options.setHideOutputs(true);
        var convention = new OpenInferenceChatModelObservationConvention(options);
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("Hi")), ChatOptions.builder().model("mistral").build()))
                .provider(AiProvider.SPRING_AI.value())
                .build();
        context.setResponse(ChatResponse.builder()
                .metadata(ChatResponseMetadata.builder().model("mistral-42").usage(new TestUsage()).build())
                .generations(List.of(new Generation(AssistantMessage.builder().content("Hello").build())))
                .build());

        assertThat(convention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.INPUT_VALUE, OpenInferenceOptions.REDACTED_PLACEHOLDER),
                KeyValue.of(SemanticConventions.OUTPUT_VALUE, OpenInferenceOptions.REDACTED_PLACEHOLDER));
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

        JSONAssert.assertEquals(toolSchema0, findKeyValue(keyValues, SemanticConventions.LLM_TOOLS + ".0." + SemanticConventions.TOOL_JSON_SCHEMA), JSONCompareMode.LENIENT);

        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.LLM_TOOLS + ".0." + SemanticConventions.TOOL_NAME, "get_weather"),
                KeyValue.of(SemanticConventions.LLM_TOOLS + ".0." + SemanticConventions.TOOL_DESCRIPTION, "Get the current weather in a location"),
                KeyValue.of(SemanticConventions.LLM_OUTPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_TOOL_CALLS + ".0." + SemanticConventions.TOOL_CALL_ID, "call_1"),
                KeyValue.of(SemanticConventions.LLM_OUTPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_TOOL_CALLS + ".0." + SemanticConventions.TOOL_CALL_FUNCTION_NAME, "get_weather"),
                KeyValue.of(SemanticConventions.LLM_OUTPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_TOOL_CALLS + ".0." + SemanticConventions.TOOL_CALL_FUNCTION_ARGUMENTS_JSON, "{\"location\":\"New York\"}")
        );
    }

    @Test
    void shouldRedactToolsWhenHideLlmTools() {
        OpenInferenceOptions options = new OpenInferenceOptions();
        options.setHideLlmTools(true);
        var convention = new OpenInferenceChatModelObservationConvention(options);
        ToolDefinition weatherTool = ToolDefinition.builder().name("get_weather").description("Get the weather").inputSchema("{}").build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("Hi")), ToolCallingChatOptions.builder()
                        .model("mistral").toolCallbacks(new TestToolCallback(weatherTool)).build()))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(KeyValue.of(SemanticConventions.LLM_TOOLS, OpenInferenceOptions.REDACTED_PLACEHOLDER));
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith(SemanticConventions.LLM_TOOLS + ".0."));
        // The input messages are not affected.
        assertThat(keyValues).contains(
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_CONTENT, "Hi"));
    }

    @Test
    void shouldKeepInvalidToolSchemaAsString() throws JSONException {
        ToolDefinition weatherTool = ToolDefinition.builder()
                .name("get_weather")
                .description("Get the weather")
                .inputSchema("location: string")
                .build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("Hi")), ToolCallingChatOptions.builder()
                        .model("mistral").toolCallbacks(new TestToolCallback(weatherTool)).build()))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);

        JSONAssert.assertEquals("""
                {"type":"function","function":{"name":"get_weather","description":"Get the weather","parameters":"location: string"}}""",
                findKeyValue(keyValues, SemanticConventions.LLM_TOOLS + ".0." + SemanticConventions.TOOL_JSON_SCHEMA),
                JSONCompareMode.STRICT);
    }

    @Test
    void shouldNotFailObservationOnMalformedContent() {
        ToolDefinition weatherTool = ToolDefinition.builder()
                .name("get_weather")
                .description("Get the weather")
                .inputSchema("location: string")
                .build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(new UserMessage("Hi")), ToolCallingChatOptions.builder()
                        .model("mistral").toolCallbacks(new TestToolCallback(weatherTool)).build()))
                .provider(AiProvider.SPRING_AI.value())
                .build();
        context.setResponse(ChatResponse.builder()
                .generations(List.of(new Generation(AssistantMessage.builder()
                        .toolCalls(List.of(new AssistantMessage.ToolCall("call_1", "function", "get_weather", "{\"location\":\"Par")))
                        .build())))
                .build());

        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(observationContext -> true);

        Observation.createNotStarted(observationConvention, () -> context, registry).observe(() -> {});

        assertThat(context.getHighCardinalityKeyValues()).contains(
                KeyValue.of(SemanticConventions.LLM_TOOLS + ".0." + SemanticConventions.TOOL_NAME, "get_weather"),
                KeyValue.of(SemanticConventions.LLM_OUTPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_TOOL_CALLS + ".0."
                        + SemanticConventions.TOOL_CALL_FUNCTION_ARGUMENTS_JSON, "{\"location\":\"Par"));
    }

    @Test
    void shouldNotFailWhenToolCallbacksAreNull() {
        List<Message> messages = List.of(new UserMessage("Hello"));
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .model("mistral")
                .toolCallbacks((List<ToolCallback>) null)
                .build();

        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(messages, options))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).startsWith(SemanticConventions.LLM_TOOLS + "."));
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
    void shouldRecordEachParallelToolResponseAsMessage() {
        List<Message> messages = List.of(
                ToolResponseMessage.builder().responses(List.of(
                        new ToolResponseMessage.ToolResponse("call_1", "weather_tool", "sunny"),
                        new ToolResponseMessage.ToolResponse("call_2", "time_tool", "10:00")
                )).build(),
                new UserMessage("Thanks")
        );
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(messages, ChatOptions.builder().model("mistral").build()))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_ROLE, "tool"),
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_TOOL_CALL_ID, "call_1"),
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_NAME, "weather_tool"),
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_CONTENT, "sunny"),
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".1." + SemanticConventions.MESSAGE_ROLE, "tool"),
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".1." + SemanticConventions.MESSAGE_TOOL_CALL_ID, "call_2"),
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".1." + SemanticConventions.MESSAGE_NAME, "time_tool"),
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".1." + SemanticConventions.MESSAGE_CONTENT, "10:00"),
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".2." + SemanticConventions.MESSAGE_ROLE, "user"),
                KeyValue.of(SemanticConventions.LLM_INPUT_MESSAGES + ".2." + SemanticConventions.MESSAGE_CONTENT, "Thanks")
        );
    }

    @Test
    void shouldUseMessageContentForTextOnlyMessages() {
        List<Message> messages = List.of(new UserMessage("Hello"));
        ChatOptions options = ChatOptions.builder().model("mistral").build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(messages, options))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        String prefix = SemanticConventions.LLM_INPUT_MESSAGES + ".0";
        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(prefix + "." + SemanticConventions.MESSAGE_CONTENT, "Hello")
        );
        // Should not have message.contents when text-only
        assertThat(observationConvention.getHighCardinalityKeyValues(context))
                .noneSatisfy(kv -> assertThat(kv.getKey()).startsWith(prefix + "." + SemanticConventions.MESSAGE_CONTENTS));
    }

    @Test
    void shouldUseMessageContentsWithMultipleMedia() {
        UserMessage userMessage = UserMessage.builder()
                .text("Compare these images")
                .media(
                        Media.builder()
                                .mimeType(Media.Format.IMAGE_PNG)
                                .data(URI.create("https://example.com/image1.png"))
                                .build(),
                        Media.builder()
                                .mimeType(Media.Format.IMAGE_JPEG)
                                .data(URI.create("https://example.com/image2.jpg"))
                                .build())
                .build();
        List<Message> messages = List.of(userMessage);
        ChatOptions options = ChatOptions.builder().model("mistral").build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(messages, options))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        String prefix = SemanticConventions.LLM_INPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_CONTENTS;
        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(prefix + ".0." + SemanticConventions.MESSAGE_CONTENT_TYPE, "text"),
                KeyValue.of(prefix + ".0." + SemanticConventions.MESSAGE_CONTENT_TEXT, "Compare these images"),
                KeyValue.of(prefix + ".1." + SemanticConventions.MESSAGE_CONTENT_TYPE, "image"),
                KeyValue.of(prefix + ".1." + SemanticConventions.MESSAGE_CONTENT_IMAGE + "." + SemanticConventions.IMAGE_URL, "https://example.com/image1.png"),
                KeyValue.of(prefix + ".2." + SemanticConventions.MESSAGE_CONTENT_TYPE, "image"),
                KeyValue.of(prefix + ".2." + SemanticConventions.MESSAGE_CONTENT_IMAGE + "." + SemanticConventions.IMAGE_URL, "https://example.com/image2.jpg")
        );
        // Should not have flat message.content when multimodal
        assertThat(observationConvention.getHighCardinalityKeyValues(context)).noneSatisfy(kv -> assertThat(kv.getKey())
                .isEqualTo(SemanticConventions.LLM_INPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_CONTENT));
    }

    @Test
    void shouldUseMessageContentsWithAudioMedia() {
        UserMessage userMessage = UserMessage.builder()
                .text("Transcribe this audio")
                .media(Media.builder()
                        .mimeType(MimeType.valueOf("audio/mpeg"))
                        .data(URI.create("https://example.com/audio.mp3"))
                        .build())
                .build();
        List<Message> messages = List.of(userMessage);
        ChatOptions options = ChatOptions.builder().model("mistral").build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(messages, options))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        String prefix = SemanticConventions.LLM_INPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_CONTENTS;
        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(prefix + ".0." + SemanticConventions.MESSAGE_CONTENT_TYPE, "text"),
                KeyValue.of(prefix + ".0." + SemanticConventions.MESSAGE_CONTENT_TEXT, "Transcribe this audio"),
                KeyValue.of(prefix + ".1." + SemanticConventions.MESSAGE_CONTENT_TYPE, "audio"),
                KeyValue.of(prefix + ".1." + SemanticConventions.MESSAGE_CONTENT_AUDIO + "." + SemanticConventions.AUDIO_URL, "https://example.com/audio.mp3"),
                KeyValue.of(prefix + ".1." + SemanticConventions.MESSAGE_CONTENT_AUDIO + "." + SemanticConventions.AUDIO_MIME_TYPE, "audio/mpeg")
        );
    }

    @Test
    void shouldRedactImagesInMultimodalMessages() {
        UserMessage userMessage = UserMessage.builder()
                .text("What's in this image?")
                .media(Media.builder()
                        .mimeType(Media.Format.IMAGE_PNG)
                        .data(URI.create("https://example.com/image.png"))
                        .build())
                .build();
        List<Message> messages = List.of(userMessage);
        ChatOptions options = ChatOptions.builder().model("mistral").build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(messages, options))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        OpenInferenceOptions redactingOptions = new OpenInferenceOptions();
        redactingOptions.setHideInputImages(true);
        var redactingConvention = new OpenInferenceChatModelObservationConvention(redactingOptions);

        String prefix = SemanticConventions.LLM_INPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_CONTENTS;
        assertThat(redactingConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(prefix + ".0." + SemanticConventions.MESSAGE_CONTENT_TYPE, "text"),
                KeyValue.of(prefix + ".0." + SemanticConventions.MESSAGE_CONTENT_TEXT, "What's in this image?"),
                KeyValue.of(prefix + ".1." + SemanticConventions.MESSAGE_CONTENT_TYPE, "image"),
                KeyValue.of(prefix + ".1." + SemanticConventions.MESSAGE_CONTENT_IMAGE + "." + SemanticConventions.IMAGE_URL, OpenInferenceOptions.REDACTED_PLACEHOLDER)
        );
    }

    @Test
    void shouldKeepBase64ImageDataUpToMaxLength() {
        // "data:image/png;base64," is 22 characters and 3 bytes encode to 4.
        UserMessage userMessage = UserMessage.builder()
                .text("What's in this image?")
                .media(Media.builder()
                        .mimeType(Media.Format.IMAGE_PNG)
                        .data(new byte[] {1, 2, 3})
                        .build())
                .build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(List.of(userMessage), ChatOptions.builder().model("mistral").build()))
                .provider(AiProvider.SPRING_AI.value())
                .build();
        String imageUrlKey = SemanticConventions.LLM_INPUT_MESSAGES + ".0."
                + SemanticConventions.MESSAGE_CONTENTS + ".1."
                + SemanticConventions.MESSAGE_CONTENT_IMAGE + "." + SemanticConventions.IMAGE_URL;

        OpenInferenceOptions exactOptions = new OpenInferenceOptions();
        exactOptions.setBase64ImageMaxLength(26);
        assertThat(findKeyValue(new OpenInferenceChatModelObservationConvention(exactOptions)
                .getHighCardinalityKeyValues(context), imageUrlKey)).isEqualTo("data:image/png;base64,AQID");

        OpenInferenceOptions shortOptions = new OpenInferenceOptions();
        shortOptions.setBase64ImageMaxLength(25);
        assertThat(findKeyValue(new OpenInferenceChatModelObservationConvention(shortOptions)
                .getHighCardinalityKeyValues(context), imageUrlKey)).isEqualTo(OpenInferenceOptions.REDACTED_PLACEHOLDER);
    }

    @Test
    void shouldRedactBase64ImageDataLongerThanMaxLength() {
        byte[] imageBytes = new byte[50_000];
        UserMessage userMessage = UserMessage.builder()
                .text("What's in this image?")
                .media(Media.builder()
                        .mimeType(Media.Format.IMAGE_PNG)
                        .data(imageBytes)
                        .build())
                .build();
        List<Message> messages = List.of(userMessage);
        ChatOptions options = ChatOptions.builder().model("mistral").build();
        ChatModelObservationContext context = ChatModelObservationContext.builder()
                .prompt(new Prompt(messages, options))
                .provider(AiProvider.SPRING_AI.value())
                .build();

        OpenInferenceOptions limitedOptions = new OpenInferenceOptions();
        limitedOptions.setBase64ImageMaxLength(100);
        var limitedConvention = new OpenInferenceChatModelObservationConvention(limitedOptions);

        String imageUrlKey = SemanticConventions.LLM_INPUT_MESSAGES + ".0."
                + SemanticConventions.MESSAGE_CONTENTS + ".1."
                + SemanticConventions.MESSAGE_CONTENT_IMAGE + "." + SemanticConventions.IMAGE_URL;
        KeyValues keyValues = limitedConvention.getHighCardinalityKeyValues(context);
        String imageUrl = findKeyValue(keyValues, imageUrlKey);

        // A truncated data URI would be invalid, so the specification requires redacting it.
        assertThat(imageUrl).isEqualTo(OpenInferenceOptions.REDACTED_PLACEHOLDER);
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
                .generations(List.of(new Generation(AssistantMessage.builder().content("Spring AI is a framework...").build())))
                .build();
        context.setResponse(response);

        String inputMessageContentKey = SemanticConventions.LLM_INPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_CONTENT;
        String outputMessageContentKey = SemanticConventions.LLM_OUTPUT_MESSAGES + ".0." + SemanticConventions.MESSAGE_CONTENT;

        assertRedactionBehavior(context, OpenInferenceOptions::setHideLlmInvocationParameters,
                SemanticConventions.LLM_INVOCATION_PARAMETERS);
        assertRedactionBehavior(context, OpenInferenceOptions::setHideInputs,
                SemanticConventions.LLM_INPUT_MESSAGES);
        assertRedactionBehavior(context, OpenInferenceOptions::setHideInputMessages,
                SemanticConventions.LLM_INPUT_MESSAGES);
        assertRedactionBehavior(context, OpenInferenceOptions::setHideInputText,
                inputMessageContentKey);
        assertRedactionBehavior(context, OpenInferenceOptions::setHideOutputs,
                SemanticConventions.LLM_OUTPUT_MESSAGES);
        assertRedactionBehavior(context, OpenInferenceOptions::setHideOutputMessages,
                SemanticConventions.LLM_OUTPUT_MESSAGES);
        assertRedactionBehavior(context, OpenInferenceOptions::setHideOutputText,
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

        assertRedactionBehavior(context, OpenInferenceOptions::setHideInputs, SemanticConventions.LLM_TOOLS);
        assertRedactionBehavior(context, OpenInferenceOptions::setHideOutputText, toolCallArgsKey);
    }

    private String findKeyValue(KeyValues keyValues, String key) {
        return keyValues.stream()
                .filter(kv -> Objects.equals(kv.getKey(), key))
                .findFirst()
                .map(KeyValue::getValue)
                .orElseThrow(() -> new AssertionError("Key not found: " + key));
    }

    private void assertRedactionBehavior(ChatModelObservationContext context,
                                         java.util.function.BiConsumer<OpenInferenceOptions, Boolean> optionSetter,
                                         String expectedKey) {
        OpenInferenceOptions redactingOptions = new OpenInferenceOptions();
        OpenInferenceChatModelObservationConvention redactingConvention =
                new OpenInferenceChatModelObservationConvention(redactingOptions);
        optionSetter.accept(redactingOptions, true);

        assertThat(redactingConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(expectedKey, OpenInferenceOptions.REDACTED_PLACEHOLDER)
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
        public Long getCacheReadInputTokens() {
            return 800L;
        }

        @Override
        public Long getCacheWriteInputTokens() {
            return 100L;
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
