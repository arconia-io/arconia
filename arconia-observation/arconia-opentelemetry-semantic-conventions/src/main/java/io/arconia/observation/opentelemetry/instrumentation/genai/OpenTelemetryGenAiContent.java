package io.arconia.observation.opentelemetry.instrumentation.genai;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.content.Media;
import org.springframework.ai.content.MediaContent;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import tools.jackson.core.type.TypeReference;

/**
 * Record types representing the OpenTelemetry GenAI semantic convention schemas for input
 * and output messages, with factory methods to convert from Spring AI types.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-input-messages.json">Input Messages Schema</a>
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-output-messages.json">Output Messages Schema</a>
 */
final class OpenTelemetryGenAiContent {

    private OpenTelemetryGenAiContent() {
    }

    // Types

    sealed interface MessagePart permits TextPart, ToolCallRequestPart, ToolCallResponsePart, BlobPart, UriPart {
    }

    record TextPart(String type, String content) implements MessagePart {
        TextPart(String content) {
            this("text", content);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ToolCallRequestPart(String type, String id, String name, Object arguments) implements MessagePart {
        ToolCallRequestPart(String id, String name, Object arguments) {
            this("tool_call", id, name, arguments);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ToolCallResponsePart(String type, String id, Object response) implements MessagePart {
        ToolCallResponsePart(String id, Object response) {
            this("tool_call_response", id, response);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record BlobPart(String type, String modality,
            @JsonProperty("mime_type") String mimeType, String content) implements MessagePart {
        BlobPart(String modality, String mimeType, String content) {
            this("blob", modality, mimeType, content);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record UriPart(String type, String modality,
            @JsonProperty("mime_type") String mimeType, String uri) implements MessagePart {
        UriPart(String modality, String mimeType, String uri) {
            this("uri", modality, mimeType, uri);
        }
    }

    record InputMessage(String role, List<MessagePart> parts) {
    }

    record OutputMessage(String role, List<MessagePart> parts,
            @JsonProperty("finish_reason") String finishReason) {
    }

    // Converters

    static List<InputMessage> fromMessages(List<Message> messages) {
        List<InputMessage> inputMessages = new ArrayList<>();
        for (Message message : messages) {
            List<MessagePart> parts = new ArrayList<>();

            if (StringUtils.hasText(message.getText())) {
                parts.add(new TextPart(message.getText()));
            }

            addMediaParts(parts, message);

            if (message instanceof AssistantMessage assistantMessage) {
                for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                    parts.add(new ToolCallRequestPart(
                            toolCall.id(), toolCall.name(), parseArguments(toolCall.arguments())));
                }
            }

            if (message instanceof ToolResponseMessage toolResponseMessage) {
                for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
                    parts.add(new ToolCallResponsePart(response.id(), response.responseData()));
                }
            }

            if (!parts.isEmpty()) {
                inputMessages.add(new InputMessage(message.getMessageType().getValue(), parts));
            }
        }
        return inputMessages;
    }

    static List<OutputMessage> fromGenerations(List<Generation> generations) {
        List<OutputMessage> outputMessages = new ArrayList<>();
        for (Generation generation : generations) {
            AssistantMessage message = generation.getOutput();
            List<MessagePart> parts = new ArrayList<>();

            if (StringUtils.hasText(message.getText())) {
                parts.add(new TextPart(message.getText()));
            }

            addMediaParts(parts, message);

            for (AssistantMessage.ToolCall toolCall : message.getToolCalls()) {
                parts.add(new ToolCallRequestPart(
                        toolCall.id(), toolCall.name(), parseArguments(toolCall.arguments())));
            }

            String finishReason = StringUtils.hasText(generation.getMetadata().getFinishReason())
                    ? generation.getMetadata().getFinishReason()
                    : "";

            outputMessages.add(new OutputMessage(message.getMessageType().getValue(), parts, finishReason));
        }
        return outputMessages;
    }

    private static void addMediaParts(List<MessagePart> parts, Message message) {
        if (!(message instanceof MediaContent mediaContent)
                || CollectionUtils.isEmpty(mediaContent.getMedia())) {
            return;
        }
        for (Media media : mediaContent.getMedia()) {
            String modality = media.getMimeType().getType();
            String mimeType = media.getMimeType().toString();
            Object data = media.getData();
            if (data instanceof byte[] bytes) {
                parts.add(new BlobPart(modality, mimeType,
                        Base64.getEncoder().encodeToString(bytes)));
            } else {
                parts.add(new UriPart(modality, mimeType, data.toString()));
            }
        }
    }


    private static Map<String, Object> parseArguments(String arguments) {
        return StringUtils.hasText(arguments)
                ? JsonParser.fromJson(arguments, new TypeReference<>() {})
                : Map.of();
    }

}
