package io.arconia.observation.openinference.ai.instrumentation;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;

/**
 * Checks on JSON text that is recorded as it is rather than parsed into a tree.
 */
final class OpenInferenceJson {

    private static final JsonFactory jsonFactory = new JsonFactory();

    private OpenInferenceJson() {}

    /**
     * Whether the text is a single well-formed JSON value, so that it can be embedded in a JSON document as it is.
     */
    static boolean isValid(String value) {
        return rootToken(value) != null;
    }

    /**
     * Whether the text is a well-formed JSON object or array, as opposed to a scalar or plain text.
     */
    static boolean isObjectOrArray(String value) {
        JsonToken token = rootToken(value);
        return token == JsonToken.START_OBJECT || token == JsonToken.START_ARRAY;
    }

    /**
     * Returns the first token of the text if it is exactly one well-formed JSON value, and null otherwise.
     */
    private static @Nullable JsonToken rootToken(String value) {
        try (JsonParser parser = jsonFactory.createParser(ObjectReadContext.empty(), value)) {
            JsonToken token = parser.nextToken();
            if (token == null || parser.skipChildren().nextToken() != null) {
                return null;
            }
            return token;
        } catch (JacksonException ex) {
            return null;
        }
    }

}
