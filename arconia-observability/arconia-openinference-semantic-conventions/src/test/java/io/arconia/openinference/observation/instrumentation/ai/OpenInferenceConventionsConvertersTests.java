package io.arconia.openinference.observation.instrumentation.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenInferenceConventionsConverters}.
 */
class OpenInferenceConventionsConvertersTests {

    @ParameterizedTest
    @CsvSource({
            "ANTHROPIC, anthropic",
            "AZURE_OPENAI, azure",
            "BEDROCK_CONVERSE, aws",
            "DEEPSEEK, deepseek",
            "MISTRAL_AI, mistralai",
            "OPENAI, openai",
            "VERTEX_AI, google",
            "OLLAMA, ollama",
            "anthropic, anthropic",
            "azure_openai, azure",
            "bedrock_converse, aws",
            "' ANTHROPIC ', anthropic",
            "'\tOPENAI\n', openai"
    })
    void toLlmProviderShouldConvertValidValues(String input, String expected) {
        String result = OpenInferenceConventionsConverters.toLlmProvider(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toLlmProviderShouldReturnInputForUnknownProvider() {
        String result = OpenInferenceConventionsConverters.toLlmProvider("unknown_provider");
        assertThat(result).isEqualTo("unknown_provider");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { " ", "  " })
    void toLlmProviderShouldThrowExceptionForEmptyInput(String input) {
        assertThatThrownBy(() -> OpenInferenceConventionsConverters.toLlmProvider(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aiProvider cannot be null or empty");
    }

    @ParameterizedTest
    @CsvSource({
            "ANTHROPIC, anthropic",
            "MISTRAL_AI, mistralai",
            "OPENAI, openai",
            "VERTEX_AI, google",
            "OLLAMA, ollama",
            "anthropic, anthropic",
            "mistral_ai, mistralai",
            "' ANTHROPIC ', anthropic",
            "'\tOPENAI\n', openai"
    })
    void toLlmSystemShouldConvertValidValues(String input, String expected) {
        String result = OpenInferenceConventionsConverters.toLlmSystem(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toLlmSystemShouldReturnInputForUnknownProvider() {
        String result = OpenInferenceConventionsConverters.toLlmSystem("unknown_provider");
        assertThat(result).isEqualTo("unknown_provider");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { " ", "  " })
    void toLlmSystemShouldThrowExceptionForEmptyInput(String input) {
        assertThatThrownBy(() -> OpenInferenceConventionsConverters.toLlmSystem(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aiProvider cannot be null or empty");
    }

    @ParameterizedTest
    @CsvSource({
            "CHAT, LLM",
            "TEXT_COMPLETION, LLM",
            "EMBEDDING, EMBEDDING",
            "FRAMEWORK, CHAIN",
            "CUSTOM_TYPE, CUSTOM_TYPE",
            "chat, LLM",
            "text_completion, LLM",
            "embedding, EMBEDDING",
            "' CHAT ', LLM",
            "'\tEMBEDDING\n', EMBEDDING"
    })
    void toSpanKindShouldConvertValidValues(String input, String expected) {
        String result = OpenInferenceConventionsConverters.toSpanKind(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toSpanKindShouldReturnInputForUnknownOperationType() {
        String result = OpenInferenceConventionsConverters.toSpanKind("unknown_operation");
        assertThat(result).isEqualTo("unknown_operation");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { " ", "  " })
    void toSpanKindShouldThrowExceptionForEmptyInput(String input) {
        assertThatThrownBy(() -> OpenInferenceConventionsConverters.toSpanKind(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operationType cannot be null or empty");
    }

}
