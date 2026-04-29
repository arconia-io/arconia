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
            "AZURE_OPENAI, azure.ai.openai",
            "BEDROCK_CONVERSE, aws.bedrock",
            "DEEPSEEK, deepseek",
            "GOOGLE_GENAI_AI, gcp.gen_ai",
            "MISTRAL_AI, mistral_ai",
            "OPENAI, openai",
            "OPENAI_SDK, openai",
            "VERTEX_AI, gcp.vertex_ai",
            "OLLAMA, ollama",
            "anthropic, anthropic",
            "azure_openai, azure.ai.openai",
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

}
