package io.arconia.observation.langsmith.instrumentation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LangSmithToolCallingObservationConvention}.
 */
class LangSmithToolCallingObservationConventionTests {

    private final LangSmithOptions options = new LangSmithOptions();
    private final LangSmithToolCallingObservationConvention convention =
            new LangSmithToolCallingObservationConvention(options);

    @Test
    void contextualNameShouldIncludeToolName() {
        ToolCallingObservationContext context = createContext("get_weather", "Get weather",
                """
                {"location": "Paris"}""");

        assertThat(convention.getContextualName(context)).isEqualTo("execute_tool get_weather");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(), "tool"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "execute_tool"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_NAME.getKey(), "get_weather")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithDescription() {
        ToolCallingObservationContext context = createContext("get_weather", "Get the current weather", "{}");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_DESCRIPTION.getKey(), "Get the current weather")
        );
    }

    @Test
    void shouldNotIncludeContentWhenDisabled() {
        options.getToolExecution().setIncludeContent(false);
        ToolCallingObservationContext context = createContext("get_weather", "Get weather",
                """
                {"location": "Paris"}""");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(LangSmithAttributes.GEN_AI_PROMPT.getKey()));
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(LangSmithAttributes.GEN_AI_COMPLETION.getKey()));
    }

    @Test
    void shouldIncludeArgumentsAsInput() throws JSONException {
        ToolCallingObservationContext context = createContext("get_weather", "Get weather",
                """
                {"location": "Paris"}""");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        String prompt = findKeyValue(keyValues, LangSmithAttributes.GEN_AI_PROMPT.getKey());
        JSONAssert.assertEquals("""
                {"location": "Paris"}""", prompt, JSONCompareMode.STRICT);
    }

    @Test
    void shouldWrapNonObjectResultAsJsonObject() throws JSONException {
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");
        context.setToolCallResult("rainy, 57°F");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        String completion = findKeyValue(keyValues, LangSmithAttributes.GEN_AI_COMPLETION.getKey());
        JSONAssert.assertEquals("""
                {"output": "rainy, 57°F"}""", completion, JSONCompareMode.STRICT);
    }

    @Test
    void shouldWrapJsonArrayResultAsJsonObject() throws JSONException {
        ToolCallingObservationContext context = createContext("get_books", "Get books", "{}");
        context.setToolCallResult("""
                [{"title": "His Dark Materials", "author": "Philip Pullman"}]""");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        String completion = findKeyValue(keyValues, LangSmithAttributes.GEN_AI_COMPLETION.getKey());
        JSONAssert.assertEquals("""
                {"output": [{"title": "His Dark Materials", "author": "Philip Pullman"}]}""",
                completion, JSONCompareMode.STRICT);
    }

    @Test
    void shouldPassThroughJsonObjectResult() throws JSONException {
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");
        context.setToolCallResult("""
                {"temperature": "57°F", "condition": "rainy"}""");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        String completion = findKeyValue(keyValues, LangSmithAttributes.GEN_AI_COMPLETION.getKey());
        JSONAssert.assertEquals("""
                {"temperature": "57°F", "condition": "rainy"}""", completion, JSONCompareMode.STRICT);
    }

    @Test
    void shouldNotIncludeOutputWhenResultIsNull() {
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(LangSmithAttributes.GEN_AI_COMPLETION.getKey()));
    }

    private String findKeyValue(KeyValues keyValues, String key) {
        return keyValues.stream()
                .filter(kv -> kv.getKey().equals(key))
                .findFirst()
                .map(KeyValue::getValue)
                .orElseThrow(() -> new AssertionError("Key not found: " + key));
    }

    private ToolCallingObservationContext createContext(String name, String description, String arguments) {
        ToolDefinition toolDefinition = ToolDefinition.builder()
                .name(name)
                .description(description)
                .inputSchema("{}")
                .build();
        return ToolCallingObservationContext.builder()
                .toolDefinition(toolDefinition)
                .toolCallArguments(arguments)
                .build();
    }

}
