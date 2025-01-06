package io.arconia.ai.core.tools.metadata;

import java.lang.reflect.Method;

/**
 * Metadata for a tool.
 */
public interface ToolMetadata {

    /**
     * The tool name. Unique within the tool set provided to a model.
     */
    String name();

    /**
     * The tool description, used by the model to decide if and when to use the tool.
     */
    String description();

    /**
     * The JSON Schema of the parameters used to call the tool.
     */
    String inputTypeSchema();

    /**
     * Whether the tool result should be returned directly or passed back to the model.
     */
    default boolean returnDirect() {
        return false;
    }

    /**
     * Create {@link ToolMetadata} from a {@link Method}.
     */
    static ToolMetadata from(Method method) {
        return DefaultToolMetadata.from(method);
    }

}
