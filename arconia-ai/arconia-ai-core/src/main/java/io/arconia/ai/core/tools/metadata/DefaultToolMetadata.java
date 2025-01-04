package io.arconia.ai.core.tools.metadata;

import java.lang.reflect.Method;

import org.springframework.util.Assert;

import io.arconia.ai.core.tools.annotation.Tool;
import io.arconia.ai.core.tools.json.JsonSchemaGenerator;
import io.arconia.ai.core.tools.util.ToolUtils;

/**
 * Default implementation of {@link ToolMetadata}.
 */
public record DefaultToolMetadata(String name, String description, String inputTypeSchema) implements ToolMetadata {

    public DefaultToolMetadata {
        Assert.hasText(name, "name cannot be null");
        Assert.hasText(description, "description cannot be null");
        Assert.hasText(inputTypeSchema, "inputTypeSchema cannot be null");
    }

    static DefaultToolMetadata from(Method method) {
        return DefaultToolMetadata.builder()
            .name(ToolUtils.getToolName(method.getAnnotation(Tool.class), method.getName()))
            .description(ToolUtils.getToolDescription(method.getAnnotation(Tool.class), method.getName()))
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

        private Builder() {
        }

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

        public DefaultToolMetadata build() {
            return new DefaultToolMetadata(name, description, inputTypeSchema);
        }

    }

}
