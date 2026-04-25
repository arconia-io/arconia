package io.arconia.observation.opentelemetry.instrumentation.genai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link OpenTelemetryGenAiConventionsConverter}.
 */
class OpenTelemetryGenAiConventionsConverterTests {

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
            "anthropic, anthropic",
            "azure_openai, azure.ai.openai",
            "' ANTHROPIC ', anthropic",
            "'\tOPENAI\n', openai"
    })
    void toProviderNameShouldConvertValidValues(String input, String expected) {
        String result = OpenTelemetryGenAiConventionsConverter.toProviderName(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toProviderNameShouldReturnInputForUnknownProvider() {
        String result = OpenTelemetryGenAiConventionsConverter.toProviderName("unknown_provider");
        assertThat(result).isEqualTo("unknown_provider");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { " ", "  " })
    void toProviderNameShouldThrowExceptionForEmptyInput(String input) {
        assertThatThrownBy(() -> OpenTelemetryGenAiConventionsConverter.toProviderName(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aiProvider cannot be null or empty");
    }

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
        String result = OpenTelemetryGenAiConventionsConverter.toOperationName(input);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void toOperationNameShouldReturnInputForUnknownOperation() {
        String result = OpenTelemetryGenAiConventionsConverter.toOperationName("unknown_operation");
        assertThat(result).isEqualTo("unknown_operation");
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { " ", "  " })
    void toOperationNameShouldThrowExceptionForEmptyInput(String input) {
        assertThatThrownBy(() -> OpenTelemetryGenAiConventionsConverter.toOperationName(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aiOperationType cannot be null or empty");
    }

}
