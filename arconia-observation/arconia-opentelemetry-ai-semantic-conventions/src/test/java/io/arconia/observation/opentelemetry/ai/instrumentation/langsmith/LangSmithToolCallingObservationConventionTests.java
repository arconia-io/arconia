package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LangSmithToolCallingObservationConvention}.
 */
class LangSmithToolCallingObservationConventionTests {

    private final OpenTelemetryAiConventionsProperties properties = new OpenTelemetryAiConventionsProperties();
    private final LangSmithToolCallingObservationConvention convention =
            new LangSmithToolCallingObservationConvention(properties);

    @Test
    void contextualNameShouldIncludeToolName() {
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", """
                {"location": "Paris}""");
        assertThat(convention.getContextualName(context)).isEqualTo("execute_tool get_weather");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "execute_tool"),
                KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM.getKey(), "spring_ai"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_NAME.getKey(), "get_weather"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_TYPE.getKey(), "function"),
                KeyValue.of("spring.ai.kind", "tool_call"),
                KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(), "tool")
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
    void shouldIncludeToolCallId() {
        ToolDefinition toolDefinition = ToolDefinition.builder()
                .name("get_weather")
                .description("Get weather")
                .inputSchema("{}")
                .build();
        ToolCallingObservationContext context = ToolCallingObservationContext.builder()
                .toolDefinition(toolDefinition)
                .toolCallArguments("{}")
                .toolCallId("call_abc123")
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_CALL_ID.getKey(), "call_abc123")
        );
    }

    @Test
    void shouldIncludeArgumentsAsInput() throws JSONException {
        properties.setIncludeToolCallContent(true);
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
        properties.setIncludeToolCallContent(true);
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");
        context.setToolCallResult("rainy, 57°F");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        String completion = findKeyValue(keyValues, LangSmithAttributes.GEN_AI_COMPLETION.getKey());
        JSONAssert.assertEquals("""
                {"output": "rainy, 57°F"}""", completion, JSONCompareMode.STRICT);
    }

    @Test
    void shouldWrapJsonArrayResultAsJsonObject() throws JSONException {
        properties.setIncludeToolCallContent(true);
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
    void shouldWrapJsonNullResultAsJsonObject() throws JSONException {
        properties.setIncludeToolCallContent(true);
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");
        context.setToolCallResult("null");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        String completion = findKeyValue(keyValues, LangSmithAttributes.GEN_AI_COMPLETION.getKey());
        JSONAssert.assertEquals("""
                {"output": "{}"}""", completion, JSONCompareMode.STRICT);
    }

    @Test
    void shouldPassThroughJsonObjectResult() throws JSONException {
        properties.setIncludeToolCallContent(true);
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
        properties.setIncludeToolCallContent(true);
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(LangSmithAttributes.GEN_AI_COMPLETION.getKey()));
    }

    @Test
    void shouldNotIncludeContentWhenDisabled() {
        properties.setIncludeToolCallContent(false);
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", """
                {"location": "Paris}""");
        context.setToolCallResult("rainy, 57°F");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(LangSmithAttributes.GEN_AI_PROMPT.getKey()));
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(LangSmithAttributes.GEN_AI_COMPLETION.getKey()));
    }

    @Test
    void shouldHandleNullToolCallResult() {
        properties.setIncludeToolCallContent(true);
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(LangSmithAttributes.GEN_AI_COMPLETION.getKey()));
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

    private String findKeyValue(KeyValues keyValues, String key) {
        return keyValues.stream()
                .filter(kv -> kv.getKey().equals(key))
                .findFirst()
                .map(KeyValue::getValue)
                .orElseThrow(() -> new AssertionError("Key not found: " + key));
    }

}
