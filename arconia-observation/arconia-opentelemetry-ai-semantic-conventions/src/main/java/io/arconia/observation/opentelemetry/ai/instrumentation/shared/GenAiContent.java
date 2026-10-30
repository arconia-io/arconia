package io.arconia.observation.opentelemetry.ai.instrumentation.shared;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.content.Media;
import org.springframework.ai.content.MediaContent;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.util.JsonHelper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Record types representing the OpenTelemetry GenAI semantic convention schemas for input
 * and output messages, with factory methods to convert from Spring AI types.
 *
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-input-messages.json">Input Messages Schema</a>
 * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-output-messages.json">Output Messages Schema</a>
 */
public final class GenAiContent {

    private static final JsonHelper jsonHelper = new JsonHelper();

    private GenAiContent() {}

    // Types

    public sealed interface MessagePart permits TextPart, ToolCallRequestPart, ToolCallResponsePart, BlobPart, UriPart {
    }

    public record TextPart(String type, String content) implements MessagePart {
        public TextPart(String content) {
            this("text", content);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolCallRequestPart(String type, String id, String name, Object arguments) implements MessagePart {
        public ToolCallRequestPart(String id, String name, Object arguments) {
            this("tool_call", id, name, arguments);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ToolCallResponsePart(String type, String id, Object response) implements MessagePart {
        public ToolCallResponsePart(String id, Object response) {
            this("tool_call_response", id, response);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BlobPart(String type, String modality,
            @JsonProperty("mime_type") String mimeType, String content) implements MessagePart {
        public BlobPart(String modality, String mimeType, String content) {
            this("blob", modality, mimeType, content);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UriPart(String type, String modality,
            @JsonProperty("mime_type") String mimeType, String uri) implements MessagePart {
        public UriPart(String modality, String mimeType, String uri) {
            this("uri", modality, mimeType, uri);
        }
    }

    public record InputMessage(String role, List<MessagePart> parts) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OutputMessage(String role, List<MessagePart> parts,
            @JsonProperty("finish_reason") @Nullable String finishReason) {
    }

    // Converters

    public static List<InputMessage> fromMessages(List<Message> messages) {
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

    public static List<OutputMessage> fromGenerations(List<Generation> generations) {
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

            String reportedFinishReason = generation.getMetadata().getFinishReason();
            if (!parts.isEmpty() || StringUtils.hasText(reportedFinishReason)) {
                outputMessages.add(new OutputMessage(message.getMessageType().getValue(), parts,
                        toFinishReason(reportedFinishReason, message)));
            }
        }
        return outputMessages;
    }

    /**
     * Returns the finish reason required by the output messages schema, using the well-known values of the
     * conventions. When the provider doesn't report one, it's inferred from the message: a tool call or a stop.
     */
    private static String toFinishReason(@Nullable String reportedFinishReason, AssistantMessage message) {
        if (StringUtils.hasText(reportedFinishReason)) {
            return GenAiConventionsConverter.toFinishReason(reportedFinishReason);
        }
        return message.getToolCalls().isEmpty() ? "stop" : "tool_call";
    }

    /**
     * Converts the tools available to the model in a request into tool definitions,
     * returning an empty list when the request has no tools.
     *
     * @see <a href="https://opentelemetry.io/docs/specs/semconv/gen-ai/gen-ai-tool-definitions.json">Tool Definitions Schema</a>
     */
    public static List<Map<String, Object>> toolDefinitions(@Nullable ChatOptions options) {
        if (!(options instanceof ToolCallingChatOptions toolCallingOptions)
                || CollectionUtils.isEmpty(toolCallingOptions.getToolCallbacks())) {
            return List.of();
        }
        List<Map<String, Object>> toolDefinitions = new ArrayList<>();
        for (ToolCallback toolCallback : toolCallingOptions.getToolCallbacks()) {
            Map<String, Object> toolDefinition = new HashMap<>();
            toolDefinition.put("type", "function");
            toolDefinition.put("name", toolCallback.getToolDefinition().name());
            toolDefinition.put("description", toolCallback.getToolDefinition().description());
            toolDefinition.put("parameters", parseInputSchema(toolCallback.getToolDefinition().inputSchema()));
            toolDefinitions.add(toolDefinition);
        }
        return toolDefinitions;
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
                parts.add(new BlobPart(modality, mimeType, Base64.getEncoder().encodeToString(bytes)));
            } else {
                parts.add(new UriPart(modality, mimeType, data.toString()));
            }
        }
    }

    /**
     * Parses the arguments of a tool call into the JSON value they represent, an empty object when there are none.
     * Arguments that aren't valid JSON, such as a tool call truncated by the model, are recorded as the original
     * string: the observation describes the model call, it must never fail it.
     */
    private static Object parseArguments(String arguments) {
        if (!StringUtils.hasText(arguments)) {
            return Map.of();
        }
        try {
            return Objects.requireNonNullElse(jsonHelper.fromJson(arguments, Object.class), Map.of());
        } catch (RuntimeException ex) {
            return arguments;
        }
    }

    /**
     * Parses the input schema of a tool into the JSON object it represents, recording a schema that isn't
     * valid JSON as the original string rather than failing the observation.
     */
    private static Object parseInputSchema(String inputSchema) {
        try {
            return Objects.requireNonNullElse(jsonHelper.fromJson(inputSchema,
                    new ParameterizedTypeReference<Map<String, Object>>() {}), Map.of());
        } catch (RuntimeException ex) {
            return inputSchema;
        }
    }

}
