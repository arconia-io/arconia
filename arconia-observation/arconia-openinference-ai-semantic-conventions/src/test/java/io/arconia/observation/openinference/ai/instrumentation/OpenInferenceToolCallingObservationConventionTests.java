package io.arconia.observation.openinference.ai.instrumentation;

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

    private final OpenInferenceOptions openInferenceOptions = new OpenInferenceOptions();
    private final OpenInferenceToolCallingObservationConvention observationConvention
            = new OpenInferenceToolCallingObservationConvention(openInferenceOptions);

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
        assertThat(observationConvention.getContextualName(context)).isEqualTo("execute_tool search");
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

    @Test
    void shouldHaveJsonOutputWhenResultIsJson() {
        var context = createContext();
        context.setToolCallResult("""
                {"temperature": 14}""");

        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.OUTPUT_VALUE, """
                        {"temperature": 14}"""),
                KeyValue.of(SemanticConventions.OUTPUT_MIME_TYPE, "application/json")
        );
    }

    @Test
    void shouldHaveTextOutputWhenResultIsPlainText() {
        var context = createContext();
        context.setToolCallResult("rainy, 57°F");

        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.OUTPUT_VALUE, "rainy, 57°F"),
                KeyValue.of(SemanticConventions.OUTPUT_MIME_TYPE, "text/plain")
        );
    }

    @Test
    void shouldNotHaveOutputWithoutResult() {
        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(createContext());

        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isIn(SemanticConventions.OUTPUT_VALUE, SemanticConventions.OUTPUT_MIME_TYPE));
    }

    private ToolCallingObservationContext createContext() {
        return ToolCallingObservationContext.builder()
                .toolDefinition(ToolDefinition.builder().name("weather").description("Get the weather").inputSchema("{}").build())
                .toolCallArguments("{}")
                .build();
    }

    private String findKeyValue(KeyValues keyValues, String key) {
        return keyValues.stream()
                .filter(kv -> Objects.equals(kv.getKey(), key))
                .findFirst()
                .map(KeyValue::getValue)
                .orElseThrow(() -> new AssertionError("Key not found: " + key));
    }

}
