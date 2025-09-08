package io.arconia.openinference.observation.instrumentation;

import java.util.Objects;

import com.arize.semconv.trace.SemanticConventions;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenInferenceToolCallingObservationConvention}.
 */
class OpenInferenceToolCallingObservationConventionTests {

    private final OpenInferenceTracingOptions tracingOptions = new OpenInferenceTracingOptions();
    private final OpenInferenceToolCallingObservationConvention observationConvention
            = new OpenInferenceToolCallingObservationConvention(tracingOptions);

    @Test
    void name() {
        assertThat(observationConvention.getName())
                .isEqualTo(OpenInferenceToolCallingObservationConvention.DEFAULT_NAME);
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
                KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, "TOOL"),
                KeyValue.of(SemanticConventions.TOOL_NAME, "search")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValues() throws JSONException {
        var toolCallArguments = """
                        {
                            "query": "Search for something"
                        }
                        """;

        var context = ToolCallingObservationContext.builder()
                .toolDefinition(ToolDefinition.builder().name("search").description("something").inputSchema("{}").build())
                .toolCallArguments(toolCallArguments)
                .build();

        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.TOOL_DESCRIPTION, "something"),
                KeyValue.of(SemanticConventions.INPUT_MIME_TYPE, "application/json")
        );

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);
        JSONAssert.assertEquals(toolCallArguments, findKeyValue(keyValues, SemanticConventions.INPUT_VALUE), JSONCompareMode.LENIENT);
    }

    private String findKeyValue(KeyValues keyValues, String key) {
        return keyValues.stream()
                .filter(kv -> Objects.equals(kv.getKey(), key))
                .findFirst()
                .map(KeyValue::getValue)
                .orElseThrow(() -> new AssertionError("Key not found: " + key));
    }

}
