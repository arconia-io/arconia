package io.arconia.observation.opentelemetry.instrumentation.genai;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryToolCallingModelObservationConvention}.
 */
class OpenTelemetryToolCallingModelObservationConventionTests {

    private final OpenTelemetryGenAiOptions options = new OpenTelemetryGenAiOptions();
    private final OpenTelemetryToolCallingModelObservationConvention convention =
            new OpenTelemetryToolCallingModelObservationConvention(options);

    @Test
    void contextualNameShouldIncludeToolName() {
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{\"location\":\"Paris\"}");

        assertThat(convention.getContextualName(context)).isEqualTo("execute_tool get_weather");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "execute_tool"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_TYPE.getKey(), "function"),
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
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{\"location\":\"Paris\"}");
        context.setToolCallResult("rainy, 57°F");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(GenAiMoreIncubatingAttributes.GEN_AI_TOOL_CALL_ARGUMENTS.getKey()));
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(GenAiMoreIncubatingAttributes.GEN_AI_TOOL_CALL_RESULT.getKey()));
    }

    @Test
    void shouldIncludeContentWhenEnabled() {
        options.getToolExecution().setIncludeContent(true);
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{\"location\":\"Paris\"}");
        context.setToolCallResult("rainy, 57°F");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiMoreIncubatingAttributes.GEN_AI_TOOL_CALL_ARGUMENTS.getKey(), "{\"location\":\"Paris\"}"),
                KeyValue.of(GenAiMoreIncubatingAttributes.GEN_AI_TOOL_CALL_RESULT.getKey(), "rainy, 57°F")
        );
    }

    @Test
    void shouldHandleNullToolCallResult() {
        options.getToolExecution().setIncludeContent(true);
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiMoreIncubatingAttributes.GEN_AI_TOOL_CALL_RESULT.getKey(), KeyValue.NONE_VALUE)
        );
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
