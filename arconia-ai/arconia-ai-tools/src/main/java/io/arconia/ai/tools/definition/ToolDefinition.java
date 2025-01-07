package io.arconia.ai.tools.definition;

import java.lang.reflect.Method;

/**
 * Definition of a tool that can be used by a model.
 */
public interface ToolDefinition {

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
     * Create a default {@link ToolDefinition} builder.
     */
    static DefaultToolDefinition.Builder builder() {
        return DefaultToolDefinition.builder();
    }

    /**
     * Create {@link ToolDefinition} from a {@link Method}.
     */
    static ToolDefinition from(Method method) {
        return DefaultToolDefinition.from(method);
    }

}
