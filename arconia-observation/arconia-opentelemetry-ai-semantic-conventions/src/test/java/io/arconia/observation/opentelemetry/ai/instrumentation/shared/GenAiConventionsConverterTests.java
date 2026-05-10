package io.arconia.observation.opentelemetry.ai.instrumentation.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link GenAiConventionsConverter}.
 * Consolidates converter tests from OTel, OpenLLMetry, and LangSmith flavors.
 */
class GenAiConventionsConverterTests {

    // toProviderName / toSystemName (same values, used by OTel and OpenLLMetry)

    @ParameterizedTest
    @CsvSource({
            "ANTHROPIC, anthropic",
            "BEDROCK_CONVERSE, aws.bedrock",
            "DEEPSEEK, deepseek",
            "GOOGLE_GENAI_AI, gcp.gen_ai",
            "MISTRAL_AI, mistral_ai",
            "OPENAI, openai",
            "VERTEX_AI, gcp.vertex_ai",
            "anthropic, anthropic",
            "' ANTHROPIC ', anthropic",
            "'\tOPENAI\n', openai"
    })
    void toProviderNameShouldConvertValidValues(String input, String expected) {
        String result = GenAiConventionsConverter.toProviderName(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toProviderNameShouldReturnInputForUnknownProvider() {
        String result = GenAiConventionsConverter.toProviderName("unknown_provider");
        assertThat(result).isEqualTo("unknown_provider");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { " ", "  " })
    void toProviderNameShouldThrowExceptionForEmptyInput(String input) {
        assertThatThrownBy(() -> GenAiConventionsConverter.toProviderName(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aiProvider cannot be null or empty");
    }

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
        String result = GenAiConventionsConverter.toProviderName(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toSystemNameShouldReturnInputForUnknownProvider() {
        String result = GenAiConventionsConverter.toProviderName("unknown_provider");
        assertThat(result).isEqualTo("unknown_provider");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { " ", "  " })
    void toSystemNameShouldThrowExceptionForEmptyInput(String input) {
        assertThatThrownBy(() -> GenAiConventionsConverter.toProviderName(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aiProvider cannot be null or empty");
    }

    // toOperationName

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
        String result = GenAiConventionsConverter.toOperationName(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toOperationNameShouldReturnInputForUnknownOperation() {
        String result = GenAiConventionsConverter.toOperationName("unknown_operation");
        assertThat(result).isEqualTo("unknown_operation");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { " ", "  " })
    void toOperationNameShouldThrowExceptionForEmptyInput(String input) {
        assertThatThrownBy(() -> GenAiConventionsConverter.toOperationName(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aiOperationType cannot be null or empty");
    }

    // toLangSmithOperationName

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
    void toLangSmithOperationNameShouldConvertValidValues(String input, String expected) {
        String result = GenAiConventionsConverter.toOperationName(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toLangSmithOperationNameShouldReturnInputForUnknownOperation() {
        String result = GenAiConventionsConverter.toProviderName("unknown_operation");
        assertThat(result).isEqualTo("unknown_operation");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { " ", "  " })
    void toLangSmithOperationNameShouldThrowExceptionForEmptyInput(String input) {
        assertThatThrownBy(() -> GenAiConventionsConverter.toOperationName(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aiOperationType cannot be null or empty");
    }

    // toLangSmithSpanKind

    @ParameterizedTest
    @CsvSource({
            "CHAT, llm",
            "TEXT_COMPLETION, llm",
            "EMBEDDING, embedding",
            "IMAGE, image",
            "chat, llm",
            "embedding, embedding",
            "' CHAT ', llm",
            "'\tEMBEDDING\n', embedding"
    })
    void toLangSmithSpanKindShouldConvertValidValues(String input, String expected) {
        String result = GenAiConventionsConverter.toLangSmithSpanKind(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toLangSmithSpanKindShouldReturnChainForUnknownOperation() {
        String result = GenAiConventionsConverter.toLangSmithSpanKind("unknown_operation");
        assertThat(result).isEqualTo("chain");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { " ", "  " })
    void toLangSmithSpanKindShouldThrowExceptionForEmptyInput(String input) {
        assertThatThrownBy(() -> GenAiConventionsConverter.toLangSmithSpanKind(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aiOperationType cannot be null or empty");
    }

}
