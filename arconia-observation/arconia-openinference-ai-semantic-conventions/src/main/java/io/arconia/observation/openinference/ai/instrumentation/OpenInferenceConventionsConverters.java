package io.arconia.observation.openinference.ai.instrumentation;

import com.arize.semconv.trace.SemanticConventions;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.util.Assert;

/**
 * Converters from Micrometer to OpenInference conventions.
 */
final class OpenInferenceConventionsConverters {

    /**
     * Convert the model provider to the OpenInference convention.
     */
    static String toLlmProvider(String aiProvider) {
        Assert.hasText(aiProvider, "aiProvider cannot be null or empty");

        AiProvider aiProviderEnum = findProvider(aiProvider);
        if (aiProviderEnum == null) {
            return aiProvider;
        }
        return switch(aiProviderEnum) {
            case AiProvider.ANTHROPIC -> SemanticConventions.LLMProvider.ANTHROPIC.getValue();
            case AiProvider.BEDROCK_CONVERSE -> SemanticConventions.LLMProvider.AWS.getValue();
            case AiProvider.DEEPSEEK -> SemanticConventions.LLMProvider.DEEPSEEK.getValue();
            case AiProvider.GOOGLE_GENAI_AI, AiProvider.VERTEX_AI -> SemanticConventions.LLMProvider.GOOGLE.getValue();
            case AiProvider.MISTRAL_AI -> SemanticConventions.LLMProvider.MISTRALAI.getValue();
            case AiProvider.OPENAI -> SemanticConventions.LLMProvider.OPENAI.getValue();
            default -> aiProviderEnum.value();
        };
    }

    /**
     * Convert the model system to the OpenInference convention.
     */
    static String toLlmSystem(String aiProvider) {
        Assert.hasText(aiProvider, "aiProvider cannot be null or empty");

        AiProvider aiProviderEnum = findProvider(aiProvider);
        if (aiProviderEnum == null) {
            return aiProvider;
        }
        return switch(aiProviderEnum) {
            case AiProvider.ANTHROPIC -> SemanticConventions.LLMSystem.ANTHROPIC.getValue();
            case AiProvider.BEDROCK_CONVERSE -> SemanticConventions.LLMSystem.AMAZON.getValue();
            case AiProvider.DEEPSEEK -> SemanticConventions.LLMSystem.DEEPSEEK.getValue();
            case AiProvider.MISTRAL_AI -> SemanticConventions.LLMSystem.MISTRALAI.getValue();
            case AiProvider.OPENAI -> SemanticConventions.LLMSystem.OPENAI.getValue();
            case AiProvider.GOOGLE_GENAI_AI, AiProvider.VERTEX_AI -> SemanticConventions.LLMSystem.VERTEXAI.getValue();
            default -> aiProviderEnum.value();
        };
    }

    /**
     * Finds the {@link AiProvider} matching the given value, such as {@code google_genai}, or constant name.
     * Spring AI reports the value, which doesn't always match the constant name.
     */
    private static @Nullable AiProvider findProvider(String aiProvider) {
        String candidate = aiProvider.strip();
        for (AiProvider provider : AiProvider.values()) {
            if (provider.value().equalsIgnoreCase(candidate) || provider.name().equalsIgnoreCase(candidate)) {
                return provider;
            }
        }
        return null;
    }

}
