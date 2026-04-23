package io.arconia.observation.openinference.instrumentation;

import com.arize.semconv.trace.SemanticConventions;

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

        try {
            AiProvider aiProviderEnum = AiProvider.valueOf(aiProvider.toUpperCase().strip());
            return switch(aiProviderEnum) {
                case AiProvider.ANTHROPIC -> SemanticConventions.LLMProvider.ANTHROPIC.getValue();
                case AiProvider.AZURE_OPENAI -> SemanticConventions.LLMProvider.AZURE.getValue();
                case AiProvider.BEDROCK_CONVERSE -> SemanticConventions.LLMProvider.AWS.getValue();
                case AiProvider.DEEPSEEK -> SemanticConventions.LLMProvider.DEEPSEEK.getValue();
                case AiProvider.GOOGLE_GENAI_AI, AiProvider.VERTEX_AI -> SemanticConventions.LLMProvider.GOOGLE.getValue();
                case AiProvider.MISTRAL_AI -> SemanticConventions.LLMProvider.MISTRALAI.getValue();
                case AiProvider.OPENAI, AiProvider.OPENAI_SDK -> SemanticConventions.LLMProvider.OPENAI.getValue();
                default -> aiProviderEnum.value();
            };
        } catch (IllegalArgumentException e) {
            return aiProvider;
        }
    }

    /**
     * Convert the model system to the OpenInference convention.
     */
    static String toLlmSystem(String aiProvider) {
        Assert.hasText(aiProvider, "aiProvider cannot be null or empty");

        try {
            AiProvider aiProviderEnum = AiProvider.valueOf(aiProvider.toUpperCase().strip());
            return switch(aiProviderEnum) {
                case AiProvider.ANTHROPIC -> SemanticConventions.LLMSystem.ANTHROPIC.getValue();
                case AiProvider.BEDROCK_CONVERSE -> SemanticConventions.LLMSystem.AMAZON.getValue();
                case AiProvider.DEEPSEEK -> SemanticConventions.LLMSystem.DEEPSEEK.getValue();
                case AiProvider.MISTRAL_AI -> SemanticConventions.LLMSystem.MISTRALAI.getValue();
                case AiProvider.OPENAI, AiProvider.OPENAI_SDK -> SemanticConventions.LLMSystem.OPENAI.getValue();
                case AiProvider.GOOGLE_GENAI_AI, AiProvider.VERTEX_AI -> SemanticConventions.LLMSystem.VERTEXAI.getValue();
                default -> aiProviderEnum.value();
            };
        } catch (IllegalArgumentException e) {
            return aiProvider;
        }
    }

}
