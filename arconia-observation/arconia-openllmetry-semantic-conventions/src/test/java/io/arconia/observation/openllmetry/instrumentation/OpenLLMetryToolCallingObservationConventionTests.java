package io.arconia.observation.openllmetry.instrumentation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLLMetryToolCallingObservationConvention}.
 */
class OpenLLMetryToolCallingObservationConventionTests {

    private final OpenLLMetryOptions openLLMetryOptions = new OpenLLMetryOptions();
    private final OpenLLMetryToolCallingObservationConvention observationConvention
            = new OpenLLMetryToolCallingObservationConvention(openLLMetryOptions);

    @Test
    void name() {
        assertThat(observationConvention.getName())
                .isEqualTo(OpenLLMetryToolCallingObservationConvention.DEFAULT_NAME);
    }

    @Test
    void contextualName() {
        var context = ToolCallingObservationContext.builder()
                .toolDefinition(ToolDefinition.builder().name("search").description("something").inputSchema("{}").build())
                .build();
        assertThat(observationConvention.getContextualName(context)).isEqualTo("tool_call search");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        var context = ToolCallingObservationContext.builder()
                .toolDefinition(ToolDefinition.builder().name("search").description("something").inputSchema("{}").build())
                .build();

        assertThat(observationConvention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, "tool"),
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_NAME, "search")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValues() {
        var toolCallArguments = """
                        {
                            "query": "Search for something"
                        }
                        """;

        var context = ToolCallingObservationContext.builder()
                .toolDefinition(ToolDefinition.builder().name("search").description("something").inputSchema("{}").build())
                .toolCallArguments(toolCallArguments)
                .build();

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, toolCallArguments)
        );
    }

    @Test
    void shouldHaveToolCallResultInOutput() {
        var context = ToolCallingObservationContext.builder()
                .toolDefinition(ToolDefinition.builder().name("search").description("something").inputSchema("{}").build())
                .toolCallArguments("{\"query\": \"test\"}")
                .toolCallResult("{\"result\": \"found\"}")
                .build();

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, "{\"result\": \"found\"}")
        );
    }

    @Test
    void shouldReturnEmptyJsonWhenToolCallResultIsNull() {
        var context = ToolCallingObservationContext.builder()
                .toolDefinition(ToolDefinition.builder().name("search").description("something").inputSchema("{}").build())
                .toolCallArguments("{\"query\": \"test\"}")
                .build();

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, "{}")
        );
    }

    @Test
    void shouldRedactContentWhenTraceContentDisabled() {
        var context = ToolCallingObservationContext.builder()
                .toolDefinition(ToolDefinition.builder().name("search").description("something").inputSchema("{}").build())
                .toolCallArguments("{\"query\": \"secret\"}")
                .toolCallResult("{\"result\": \"sensitive\"}")
                .build();

        OpenLLMetryOptions redactingOptions = new OpenLLMetryOptions();
        redactingOptions.setTraceContent(false);
        var redactingConvention = new OpenLLMetryToolCallingObservationConvention(redactingOptions);

        KeyValues keyValues = redactingConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, OpenLLMetryOptions.REDACTED_PLACEHOLDER),
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, OpenLLMetryOptions.REDACTED_PLACEHOLDER)
        );
    }

}
