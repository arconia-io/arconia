package io.arconia.observation.openllmetry.instrumentation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.ai.observation.conventions.AiObservationAttributes;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLLMetryToolCallingObservationConvention}.
 */
class OpenLLMetryToolCallingObservationConventionTests {

    private final OpenLLMetryOptions openLLMetryOptions = new OpenLLMetryOptions();
    private final OpenLLMetryToolCallingObservationConvention convention
            = new OpenLLMetryToolCallingObservationConvention(openLLMetryOptions);

    @Test
    void shouldHaveName() {
        assertThat(convention.getName())
                .isEqualTo(OpenLLMetryToolCallingObservationConvention.DEFAULT_NAME);
    }

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
                KeyValue.of(AiObservationAttributes.AI_OPERATION_TYPE.value(), "execute_tool"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_TYPE.getKey(), "function"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_NAME.getKey(), "get_weather"),
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, "tool"),
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_NAME, "get_weather")
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
        openLLMetryOptions.getToolExecution().setIncludeContent(false);
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{\"location\":\"Paris\"}");
        context.setToolCallResult("rainy, 57°F");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT));
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT));
    }

    @Test
    void shouldIncludeContentWhenEnabled() {
        openLLMetryOptions.getToolExecution().setIncludeContent(true);
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{\"location\":\"Paris\"}");
        context.setToolCallResult("rainy, 57°F");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, "{\"location\":\"Paris\"}"),
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, "rainy, 57°F")
        );
    }

    @Test
    void shouldOmitOutputWhenToolCallResultIsNull() {
        openLLMetryOptions.getToolExecution().setIncludeContent(true);
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT));
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
