package io.arconia.observation.langsmith.instrumentation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LangSmithConventionsConverter}.
 */
class LangSmithConventionsConverterTests {

    // toSystemName

    @ParameterizedTest
    @CsvSource({
            "ANTHROPIC, anthropic",
            "AZURE_OPENAI, azure_openai",
            "BEDROCK_CONVERSE, aws_bedrock",
            "DEEPSEEK, deepseek",
            "GOOGLE_GENAI_AI, gcp_gen_ai",
            "MISTRAL_AI, mistral_ai",
            "OPENAI, openai",
            "OPENAI_SDK, openai",
            "VERTEX_AI, gcp_vertex_ai",
            "anthropic, anthropic",
            "azure_openai, azure_openai",
            "' ANTHROPIC ', anthropic",
            "'\tOPENAI\n', openai"
    })
    void toSystemNameShouldConvertValidValues(String input, String expected) {
        String result = LangSmithConventionsConverter.toSystemName(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toSystemNameShouldReturnInputForUnknownProvider() {
        String result = LangSmithConventionsConverter.toSystemName("unknown_provider");
        assertThat(result).isEqualTo("unknown_provider");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { " ", "  " })
    void toSystemNameShouldThrowExceptionForEmptyInput(String input) {
        assertThatThrownBy(() -> LangSmithConventionsConverter.toSystemName(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aiProvider cannot be null or empty");
    }

    // toSpanKind

    @ParameterizedTest
    @CsvSource({
            "CHAT, llm",
            "TEXT_COMPLETION, llm",
            "EMBEDDING, embedding",
            "IMAGE, chain",
            "chat, llm",
            "embedding, embedding",
            "' CHAT ', llm",
            "'\tEMBEDDING\n', embedding"
    })
    void toSpanKindShouldConvertValidValues(String input, String expected) {
        String result = LangSmithConventionsConverter.toSpanKind(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toSpanKindShouldReturnChainForUnknownOperation() {
        String result = LangSmithConventionsConverter.toSpanKind("unknown_operation");
        assertThat(result).isEqualTo("chain");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { " ", "  " })
    void toSpanKindShouldThrowExceptionForEmptyInput(String input) {
        assertThatThrownBy(() -> LangSmithConventionsConverter.toSpanKind(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aiOperationType cannot be null or empty");
    }

    // toOperationName

    @ParameterizedTest
    @CsvSource({
            "CHAT, chat",
            "EMBEDDING, embeddings",
            "IMAGE, generate_content",
            "TEXT_COMPLETION, text_completion",
            "chat, chat",
            "embedding, embeddings",
            "' CHAT ', chat",
            "'\tEMBEDDING\n', embeddings"
    })
    void toOperationNameShouldConvertValidValues(String input, String expected) {
        String result = LangSmithConventionsConverter.toOperationName(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toOperationNameShouldReturnInputForUnknownOperation() {
        String result = LangSmithConventionsConverter.toOperationName("unknown_operation");
        assertThat(result).isEqualTo("unknown_operation");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { " ", "  " })
    void toOperationNameShouldThrowExceptionForEmptyInput(String input) {
        assertThatThrownBy(() -> LangSmithConventionsConverter.toOperationName(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aiOperationType cannot be null or empty");
    }

}
