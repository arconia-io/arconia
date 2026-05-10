package io.arconia.observation.opentelemetry.ai.instrumentation.openllmetry;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLLMetryToolCallingObservationConvention}.
 */
class OpenLLMetryToolCallingObservationConventionTests {

    private final OpenTelemetryAiConventionsProperties properties = new OpenTelemetryAiConventionsProperties();
    private final OpenLLMetryToolCallingObservationConvention convention =
            new OpenLLMetryToolCallingObservationConvention(properties);

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
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM, "spring_ai"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_NAME.getKey(), "get_weather"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_TYPE.getKey(), "function"),
                KeyValue.of("spring.ai.kind", "tool_call")
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
    void shouldNotIncludeContentWhenDisabled() {
        properties.setIncludeToolCallContent(false);
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", """
                {"location": "Paris}""");
        context.setToolCallResult("rainy, 57°F");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT));
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT));
    }

    @Test
    void shouldIncludeContentWhenEnabled() {
        properties.setIncludeToolCallContent(true);
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", """
                {"location": "Paris}""");
        context.setToolCallResult("rainy, 57°F");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, """
                        {"location": "Paris}"""),
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, "rainy, 57°F")
        );
    }

    @Test
    void shouldHandleNullToolCallResult() {
        properties.setIncludeToolCallContent(true);
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
