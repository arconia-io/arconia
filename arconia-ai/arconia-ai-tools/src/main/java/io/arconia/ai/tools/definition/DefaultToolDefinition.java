package io.arconia.ai.tools.definition;

import org.springframework.util.Assert;

/**
 * Default implementation of {@link ToolDefinition}.
 */
public record DefaultToolDefinition(String name, String description, String inputTypeSchema) implements ToolDefinition {

    public DefaultToolDefinition {
        Assert.hasText(name, "name cannot be null or empty");
        Assert.hasText(description, "description cannot be null or empty");
        Assert.hasText(inputTypeSchema, "inputTypeSchema cannot be null or empty");
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
