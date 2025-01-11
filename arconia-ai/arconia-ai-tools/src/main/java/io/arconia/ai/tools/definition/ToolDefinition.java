package io.arconia.ai.tools.definition;

import java.lang.reflect.Method;

import io.arconia.ai.tools.json.JsonSchemaGenerator;
import io.arconia.ai.tools.util.ToolUtils;

/**
 * Definition used by the AI model to determine when and how to call the tool.
 */
public interface ToolDefinition {

    /**
     * The tool name. Unique within the tool set provided to a model.
     */
    String name();

    /**
     * The tool description, used by the AI model to determine what the tool does.
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
     * Create a default {@link ToolDefinition} instance from a {@link Method}.
     */
    static ToolDefinition from(Method method) {
        return DefaultToolDefinition.builder()
                .name(ToolUtils.getToolName(method))
                .description(ToolUtils.getToolDescription(method))
                .inputTypeSchema(JsonSchemaGenerator.generateForMethodInput(method))
                .build();
    }

}
