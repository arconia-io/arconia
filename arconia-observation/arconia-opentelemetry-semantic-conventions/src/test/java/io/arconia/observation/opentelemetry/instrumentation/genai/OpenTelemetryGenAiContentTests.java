package io.arconia.observation.opentelemetry.instrumentation.genai;

import java.net.URI;
import java.util.List;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.content.Media;
import org.springframework.ai.util.json.JsonParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryGenAiContent}.
 */
class OpenTelemetryGenAiContentTests {

    // fromMessages

    @Test
    void fromMessagesShouldConvertTextMessages() throws JSONException {
        List<Message> messages = List.of(
                new SystemMessage("You are a helpful assistant"),
                new UserMessage("Tell me a joke")
        );

        var result = OpenTelemetryGenAiContent.fromMessages(messages);

        assertThat(result).hasSize(2);

        String json = JsonParser.toJson(result);
        JSONAssert.assertEquals("""
                [
                  {"role": "system", "parts": [{"type": "text", "content": "You are a helpful assistant"}]},
                  {"role": "user", "parts": [{"type": "text", "content": "Tell me a joke"}]}
                ]""", json, JSONCompareMode.STRICT);
    }

    @Test
    void fromMessagesShouldConvertAssistantWithToolCalls() throws JSONException {
        AssistantMessage assistantMessage = AssistantMessage.builder()
                .content("I'll check the weather")
                .toolCalls(List.of(
                        new AssistantMessage.ToolCall("call_1", "function", "get_weather", "{\"location\":\"Paris\"}")
                ))
                .build();

        var result = OpenTelemetryGenAiContent.fromMessages(List.of(assistantMessage));

        String json = JsonParser.toJson(result);
        JSONAssert.assertEquals("""
                [
                  {
                    "role": "assistant",
                    "parts": [
                      {"type": "text", "content": "I'll check the weather"},
                      {"type": "tool_call", "id": "call_1", "name": "get_weather", "arguments": {"location": "Paris"}}
                    ]
                  }
                ]""", json, JSONCompareMode.STRICT);
    }

    @Test
    void fromMessagesShouldConvertToolResponseMessages() throws JSONException {
        ToolResponseMessage toolResponse = ToolResponseMessage.builder()
                .responses(List.of(
                        new ToolResponseMessage.ToolResponse("call_1", "get_weather", "rainy, 57°F")
                ))
                .build();

        var result = OpenTelemetryGenAiContent.fromMessages(List.of(toolResponse));

        String json = JsonParser.toJson(result);
        JSONAssert.assertEquals("""
                [
                  {
                    "role": "tool",
                    "parts": [
                      {"type": "tool_call_response", "id": "call_1", "response": "rainy, 57°F"}
                    ]
                  }
                ]""", json, JSONCompareMode.STRICT);
    }

    @Test
    void fromMessagesShouldConvertMultimodalWithUri() throws JSONException {
        UserMessage userMessage = UserMessage.builder()
                .text("What's in this image?")
                .media(Media.builder()
                        .mimeType(Media.Format.IMAGE_PNG)
                        .data(URI.create("https://example.com/image.png"))
                        .build())
                .build();

        var result = OpenTelemetryGenAiContent.fromMessages(List.of(userMessage));

        String json = JsonParser.toJson(result);
        JSONAssert.assertEquals("""
                [
                  {
                    "role": "user",
                    "parts": [
                      {"type": "text", "content": "What's in this image?"},
                      {"type": "uri", "modality": "image", "mime_type": "image/png", "uri": "https://example.com/image.png"}
                    ]
                  }
                ]""", json, JSONCompareMode.STRICT);
    }

    @Test
    void fromMessagesShouldConvertMultimodalWithBlob() throws JSONException {
        byte[] imageBytes = new byte[] { 1, 2, 3, 4 };
        UserMessage userMessage = UserMessage.builder()
                .text("Describe this")
                .media(Media.builder()
                        .mimeType(Media.Format.IMAGE_PNG)
                        .data(imageBytes)
                        .build())
                .build();

        var result = OpenTelemetryGenAiContent.fromMessages(List.of(userMessage));

        String json = JsonParser.toJson(result);
        JSONAssert.assertEquals("""
                [
                  {
                    "role": "user",
                    "parts": [
                      {"type": "text", "content": "Describe this"},
                      {"type": "blob", "modality": "image", "mime_type": "image/png", "content": "AQIDBA=="}
                    ]
                  }
                ]""", json, JSONCompareMode.STRICT);
    }

    @Test
    void fromMessagesShouldReturnEmptyForEmptyList() {
        var result = OpenTelemetryGenAiContent.fromMessages(List.of());
        assertThat(result).isEmpty();
    }

    // fromGenerations

    @Test
    void fromGenerationsShouldConvertTextOutput() throws JSONException {
        Generation generation = new Generation(
                AssistantMessage.builder().content("Here's a joke!").build(),
                ChatGenerationMetadata.builder().finishReason("stop").build()
        );

        var result = OpenTelemetryGenAiContent.fromGenerations(List.of(generation));

        String json = JsonParser.toJson(result);
        JSONAssert.assertEquals("""
                [
                  {
                    "role": "assistant",
                    "parts": [{"type": "text", "content": "Here's a joke!"}],
                    "finish_reason": "stop"
                  }
                ]""", json, JSONCompareMode.STRICT);
    }

    @Test
    void fromGenerationsShouldConvertToolCallOutput() throws JSONException {
        AssistantMessage assistantMessage = AssistantMessage.builder()
                .toolCalls(List.of(
                        new AssistantMessage.ToolCall("call_1", "function", "get_weather", "{\"location\":\"Paris\"}")
                ))
                .build();
        Generation generation = new Generation(
                assistantMessage,
                ChatGenerationMetadata.builder().finishReason("tool_call").build()
        );

        var result = OpenTelemetryGenAiContent.fromGenerations(List.of(generation));

        String json = JsonParser.toJson(result);
        JSONAssert.assertEquals("""
                [
                  {
                    "role": "assistant",
                    "parts": [
                      {"type": "tool_call", "id": "call_1", "name": "get_weather", "arguments": {"location": "Paris"}}
                    ],
                    "finish_reason": "tool_call"
                  }
                ]""", json, JSONCompareMode.STRICT);
    }

    @Test
    void fromGenerationsShouldHandleMissingFinishReason() throws JSONException {
        Generation generation = new Generation(
                AssistantMessage.builder().content("Hello").build()
        );

        var result = OpenTelemetryGenAiContent.fromGenerations(List.of(generation));

        String json = JsonParser.toJson(result);
        JSONAssert.assertEquals("""
                [
                  {
                    "role": "assistant",
                    "parts": [{"type": "text", "content": "Hello"}],
                    "finish_reason": ""
                  }
                ]""", json, JSONCompareMode.STRICT);
    }

    @Test
    void fromGenerationsShouldConvertMultipleChoices() throws JSONException {
        Generation gen1 = new Generation(
                AssistantMessage.builder().content("Answer 1").build(),
                ChatGenerationMetadata.builder().finishReason("stop").build()
        );
        Generation gen2 = new Generation(
                AssistantMessage.builder().content("Answer 2").build(),
                ChatGenerationMetadata.builder().finishReason("stop").build()
        );

        var result = OpenTelemetryGenAiContent.fromGenerations(List.of(gen1, gen2));

        assertThat(result).hasSize(2);

        String json = JsonParser.toJson(result);
        JSONAssert.assertEquals("""
                [
                  {"role": "assistant", "parts": [{"type": "text", "content": "Answer 1"}], "finish_reason": "stop"},
                  {"role": "assistant", "parts": [{"type": "text", "content": "Answer 2"}], "finish_reason": "stop"}
                ]""", json, JSONCompareMode.STRICT);
    }

}
