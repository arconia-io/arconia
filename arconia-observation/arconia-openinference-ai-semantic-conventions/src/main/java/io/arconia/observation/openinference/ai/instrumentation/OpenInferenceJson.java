package io.arconia.observation.openinference.ai.instrumentation;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.util.StringUtils;

/**
 * Cheap checks on JSON text, for content that is recorded as it is rather than parsed.
 */
final class OpenInferenceJson {

    private static final JsonMapper jsonMapper = JsonMapper.builder().build();

    private OpenInferenceJson() {}

    /**
     * Whether the text is well-formed JSON, checked by tokenizing it without building a tree:
     * what matters is that it can be embedded in a JSON document as it is.
     */
    static boolean isValid(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try (JsonParser parser = jsonMapper.createParser(value)) {
            while (parser.nextToken() != null) {
                // Tokenizing the whole text is the validation.
            }
            return true;
        } catch (JacksonException ex) {
            return false;
        }
    }

    /**
     * Whether the text looks like a JSON object or array, judging by its first and last characters.
     * It tells structured content from plain text without parsing it, which is enough to pick a MIME type.
     */
    static boolean isObjectOrArray(String value) {
        String trimmed = value.strip();
        return trimmed.length() >= 2
                && ((trimmed.charAt(0) == '{' && trimmed.charAt(trimmed.length() - 1) == '}')
                || (trimmed.charAt(0) == '[' && trimmed.charAt(trimmed.length() - 1) == ']'));
    }

    /**
     * Whether the text looks like a JSON object, judging by its first and last characters.
     */
    static boolean isObject(String value) {
        String trimmed = value.strip();
        return trimmed.length() >= 2 && trimmed.charAt(0) == '{' && trimmed.charAt(trimmed.length() - 1) == '}';
    }

}
