package io.arconia.ai.tools.definition;

import java.lang.reflect.Method;

import org.springframework.util.Assert;

import io.arconia.ai.tools.json.JsonSchemaGenerator;
import io.arconia.ai.tools.util.ToolUtils;

/**
 * Default implementation of {@link ToolDefinition}.
 */
public record DefaultToolDefinition(String name, String description, String inputTypeSchema) implements ToolDefinition {

    public DefaultToolDefinition {
        Assert.hasText(name, "name cannot be null");
        Assert.hasText(description, "description cannot be null");
        Assert.hasText(inputTypeSchema, "inputTypeSchema cannot be null");
    }

    static DefaultToolDefinition from(Method method) {
        return DefaultToolDefinition.builder()
            .name(ToolUtils.getToolName(method))
            .description(ToolUtils.getToolDescription(method))
            .inputTypeSchema(JsonSchemaGenerator.generate(method))
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String name;

        private String description;

        private String inputTypeSchema;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder inputTypeSchema(String inputTypeSchema) {
            this.inputTypeSchema = inputTypeSchema;
            return this;
        }

        public DefaultToolDefinition build() {
            return new DefaultToolDefinition(name, description, inputTypeSchema);
        }

    }

}
