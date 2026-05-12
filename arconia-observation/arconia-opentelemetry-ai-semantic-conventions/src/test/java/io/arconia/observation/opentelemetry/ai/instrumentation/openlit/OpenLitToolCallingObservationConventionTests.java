package io.arconia.observation.opentelemetry.ai.instrumentation.openlit;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLitToolCallingObservationConvention}.
 */
class OpenLitToolCallingObservationConventionTests {

    private final OpenTelemetryAiConventionsProperties properties = new OpenTelemetryAiConventionsProperties();
    private final OpenLitToolCallingObservationConvention convention =
            new OpenLitToolCallingObservationConvention(properties);

    @Test
    void contextualNameShouldIncludeToolName() {
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");
        assertThat(convention.getContextualName(context)).isEqualTo("execute_tool get_weather");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");

        KeyValues keyValues = convention.getLowCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "execute_tool"),
                KeyValue.of(OpenLitAttributes.GEN_AI_SYSTEM, "spring_ai"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_NAME.getKey(), "get_weather"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_TOOL_TYPE.getKey(), "function"),
                KeyValue.of("spring.ai.kind", "tool_call")
        );
    }

    @Test
    void shouldNotHaveOtelProviderNameKey() {
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");

        assertThat(convention.getLowCardinalityKeyValues(context)).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey()));
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
    void shouldUseToolArgsKeyInsteadOfToolCallArguments() {
        properties.setIncludeToolCallContent(true);
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", """
                {"location": "Paris"}""");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(OpenLitAttributes.GEN_AI_TOOL_ARGS, """
                        {"location": "Paris"}""")
        );
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(GenAiAttributes.GEN_AI_TOOL_CALL_ARGUMENTS.getKey()));
    }

    @Test
    void shouldIncludeToolCallResultWhenContentEnabled() {
        properties.setIncludeToolCallContent(true);
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");
        context.setToolCallResult("rainy, 57°F");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiAttributes.GEN_AI_TOOL_CALL_RESULT.getKey(), "rainy, 57°F")
        );
    }

    @Test
    void shouldNotIncludeToolCallResultWhenContentDisabled() {
        properties.setIncludeToolCallContent(false);
        ToolCallingObservationContext context = createContext("get_weather", "Get weather", "{}");
        context.setToolCallResult("rainy, 57°F");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(GenAiAttributes.GEN_AI_TOOL_CALL_RESULT.getKey()));
    }

    // Helpers

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
