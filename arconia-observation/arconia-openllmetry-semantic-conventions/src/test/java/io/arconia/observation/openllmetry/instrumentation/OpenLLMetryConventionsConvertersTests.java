package io.arconia.observation.openllmetry.instrumentation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenLLMetryConventionsConverters}.
 */
class OpenLLMetryConventionsConvertersTests {

    @ParameterizedTest
    @CsvSource({
            "ANTHROPIC, anthropic",
            "BEDROCK_CONVERSE, aws.bedrock",
            "DEEPSEEK, deepseek",
            "GOOGLE_GENAI_AI, gcp.gen_ai",
            "MISTRAL_AI, mistral_ai",
            "OPENAI, openai",
            "VERTEX_AI, gcp.vertex_ai",
            "OLLAMA, ollama",
            "anthropic, anthropic",
            "bedrock_converse, aws.bedrock",
            "' ANTHROPIC ', anthropic",
            "'\tOPENAI\n', openai"
    })
    void toSystemNameShouldConvertValidValues(String input, String expected) {
        String result = OpenLLMetryConventionsConverters.toSystemName(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toSystemNameShouldReturnInputForUnknownProvider() {
        String result = OpenLLMetryConventionsConverters.toSystemName("unknown_provider");
        assertThat(result).isEqualTo("unknown_provider");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { " ", "  " })
    void toSystemNameShouldThrowExceptionForEmptyInput(String input) {
        assertThatThrownBy(() -> OpenLLMetryConventionsConverters.toSystemName(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aiProvider cannot be null or empty");
    }

    @ParameterizedTest
    @CsvSource({
            "CHAT, chat",
            "EMBEDDING, embeddings",
            "IMAGE, image",
            "TEXT_COMPLETION, text_completion",
            "chat, chat",
            "embedding, embeddings",
            "' CHAT ', chat",
            "'\tEMBEDDING\n', embeddings"
    })
    void toOperationNameShouldConvertValidValues(String input, String expected) {
        String result = OpenLLMetryConventionsConverters.toOperationName(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toOperationNameShouldReturnInputForUnknownOperation() {
        String result = OpenLLMetryConventionsConverters.toOperationName("unknown_operation");
        assertThat(result).isEqualTo("unknown_operation");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { " ", "  " })
    void toOperationNameShouldThrowExceptionForEmptyInput(String input) {
        assertThatThrownBy(() -> OpenLLMetryConventionsConverters.toOperationName(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aiOperationType cannot be null or empty");
    }

}
