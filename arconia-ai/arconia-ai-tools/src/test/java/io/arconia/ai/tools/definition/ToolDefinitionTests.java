package io.arconia.ai.tools.definition;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.arconia.ai.tools.annotation.Tool;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ToolDefinition}.
 */
class ToolDefinitionTests {

    @Test
    void shouldCreateDefaultToolDefinitionBuilder() {
        var toolDefinition = ToolDefinition.builder()
            .name("name")
            .description("description")
            .inputTypeSchema("{}")
            .build();
        assertThat(toolDefinition.name()).isEqualTo("name");
        assertThat(toolDefinition.description()).isEqualTo("description");
        assertThat(toolDefinition.inputTypeSchema()).isEqualTo("{}");
    }

    @Test
    void shouldCreateToolDefinitionFromMethod() {
        var toolDefinition = ToolDefinition.from(Tools.class.getDeclaredMethods()[0]);
        assertThat(toolDefinition.name()).isEqualTo("mySuperTool");
        assertThat(toolDefinition.description()).isEqualTo("Test description");
        assertThat(toolDefinition.inputTypeSchema()).isEqualToIgnoringWhitespace("""
                {
                    "$schema" : "https://json-schema.org/draft/2020-12/schema",
                    "type" : "object",
                    "properties" : {
                      "input" : {
                        "type" : "string"
                      }
                    },
                    "required" : [ "input" ],
                    "additionalProperties" : false
                }
                """);
    }

    static class Tools {

        @Tool("Test description")
        public List<String> mySuperTool(String input) {
            return List.of(input);
        }

    }

}
