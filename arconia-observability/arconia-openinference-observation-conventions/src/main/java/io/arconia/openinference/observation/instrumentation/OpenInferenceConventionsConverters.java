package io.arconia.openinference.observation.instrumentation;

import com.arize.semconv.trace.SemanticConventions;

import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.util.Assert;

/**
 * Converters from Micrometer to OpenInference conventions.
 */
final class OpenInferenceConventionsConverters {

    static String toLlmProvider(String aiProvider) {
        Assert.hasText(aiProvider, "aiProvider cannot be null or empty");

        try {
            AiProvider aiProviderEnum = AiProvider.valueOf(aiProvider.toUpperCase().strip());
            return switch(aiProviderEnum) {
                case AiProvider.ANTHROPIC -> SemanticConventions.LLMProvider.ANTHROPIC.getValue();
                case AiProvider.AZURE_OPENAI -> SemanticConventions.LLMProvider.AZURE.getValue();
                case AiProvider.BEDROCK_CONVERSE -> SemanticConventions.LLMProvider.AWS.getValue();
                case AiProvider.DEEPSEEK -> SemanticConventions.LLMProvider.DEEPSEEK.getValue();
                case AiProvider.MISTRAL_AI -> SemanticConventions.LLMProvider.MISTRALAI.getValue();
                case AiProvider.OPENAI -> SemanticConventions.LLMProvider.OPENAI.getValue();
                case AiProvider.VERTEX_AI -> SemanticConventions.LLMProvider.GOOGLE.getValue();
                default -> aiProviderEnum.value();
            };
        } catch (IllegalArgumentException e) {
            return aiProvider;
        }
    }

    static String toLlmSystem(String aiProvider) {
        Assert.hasText(aiProvider, "aiProvider cannot be null or empty");

        try {
            AiProvider aiProviderEnum = AiProvider.valueOf(aiProvider.toUpperCase().strip());
            return switch(aiProviderEnum) {
                case AiProvider.ANTHROPIC -> SemanticConventions.LLMProvider.ANTHROPIC.getValue();
                case AiProvider.MISTRAL_AI -> SemanticConventions.LLMProvider.MISTRALAI.getValue();
                case AiProvider.OPENAI -> SemanticConventions.LLMProvider.OPENAI.getValue();
                case AiProvider.VERTEX_AI -> SemanticConventions.LLMProvider.GOOGLE.getValue();
                default -> aiProviderEnum.value();
            };
        } catch (IllegalArgumentException e) {
            return aiProvider;
        }
    }

    static String toSpanKind(String aiOperationType) {
        Assert.hasText(aiOperationType, "operationType cannot be null or empty");

        try {
            AiOperationType aiOperationTypeEnum = AiOperationType.valueOf(aiOperationType.toUpperCase().strip());
            return switch(aiOperationTypeEnum) {
                case AiOperationType.CHAT, AiOperationType.TEXT_COMPLETION -> SemanticConventions.OpenInferenceSpanKind.LLM.getValue();
                case AiOperationType.EMBEDDING -> SemanticConventions.OpenInferenceSpanKind.EMBEDDING.getValue();
                case AiOperationType.FRAMEWORK -> SemanticConventions.OpenInferenceSpanKind.CHAIN.getValue();
                default -> aiOperationTypeEnum.value();
            };
        } catch (IllegalArgumentException e) {
            return aiOperationType;
        }
    }

}
