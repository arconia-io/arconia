package io.arconia.observation.langsmith.instrumentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.ai.chat.messages.AssistantMessage;
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
 * Unit tests for {@link LangSmithChatModelObservationConvention}.
 */
class LangSmithChatModelObservationConventionTests {

    private final LangSmithOptions options = new LangSmithOptions();
    private final LangSmithChatModelObservationConvention convention =
            new LangSmithChatModelObservationConvention(options);

    @Test
    void shouldHaveName() {
        assertThat(convention.getName())
                .isEqualTo(LangSmithChatModelObservationConvention.DEFAULT_NAME);
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
                KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(), "llm"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "chat"),
                KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM.getKey(), "spring_ai"),
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
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_MAX_TOKENS.getKey(), "1000"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_TEMPERATURE.getKey(), "0.7"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_TOP_P.getKey(), "0.9"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_FREQUENCY_PENALTY.getKey(), "0.5"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_PRESENCE_PENALTY.getKey(), "0.3")
        );
    }

    @Test
    void shouldIncludeToolDefinitionsWhenEnabled() throws JSONException {
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
        String toolDefs = findKeyValue(keyValues, LangSmithAttributes.TOOLS.getKey());
        JSONAssert.assertEquals("""
                [{"type": "function", "name": "get_weather", "description": "Get weather",
                  "parameters": {"type": "object", "properties": {"location": {"type": "string"}}}}]""",
                toolDefs, JSONCompareMode.LENIENT);
    }

    @Test
    void shouldNotIncludeToolDefinitionsWhenDisabled() {
        options.getInference().setIncludeToolDefinitions(false);

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
                assertThat(kv.getKey()).isEqualTo(LangSmithAttributes.TOOLS.getKey()));
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

    private void setResponse(ChatModelObservationContext context, String model, String content, String finishReason) {
        ChatResponse response = ChatResponse.builder()
                .metadata(ChatResponseMetadata.builder()
                        .id("resp-1")
                        .model(model)
                        .usage(new TestUsage())
                        .build())
                .generations(List.of(new Generation(
                        AssistantMessage.builder().content(content).build(),
                        org.springframework.ai.chat.metadata.ChatGenerationMetadata.builder()
                                .finishReason(finishReason).build())))
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

    static class TestUsage implements Usage {
        @Override
        public Integer getPromptTokens() { return 1000; }
        @Override
        public Integer getCompletionTokens() { return 500; }
        @Override
        public Integer getTotalTokens() { return 1500; }
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
