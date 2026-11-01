package io.arconia.observation.opentelemetry.ai.instrumentation.shared;

import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;

/**
 * Checks on JSON text that is recorded as it is rather than parsed into a tree.
 */
public final class GenAiJson {

    private static final JsonFactory jsonFactory = new JsonFactory();

    private GenAiJson() {}

    /**
     * Whether the text is a single well-formed JSON value, so that it can be embedded in a JSON document as it is.
     */
    public static boolean isValid(String value) {
        return rootToken(value) != null;
    }

    /**
     * Whether the text is a well-formed JSON object.
     */
    public static boolean isObject(String value) {
        return rootToken(value) == JsonToken.START_OBJECT;
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
