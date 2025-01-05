package io.arconia.ai.core.tools;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.arconia.ai.core.tools.util.ToolUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ToolUtils}.
 */
class ToolUtilsTests {

    @Test
    void shouldDetectDuplicateToolNames() {
        ToolCallback callback1 = new TestToolCallback("tool_a");
        ToolCallback callback2 = new TestToolCallback("tool_a");
        ToolCallback callback3 = new TestToolCallback("tool_b");

        List<String> duplicates = ToolUtils.getDuplicateToolNames(callback1, callback2, callback3);

        assertThat(duplicates).isNotEmpty();
        assertThat(duplicates).contains("tool_a");
    }

    @Test
    void shouldNotDetectDuplicateToolNames() {
        ToolCallback callback1 = new TestToolCallback("tool_a");
        ToolCallback callback2 = new TestToolCallback("tool_b");
        ToolCallback callback3 = new TestToolCallback("tool_c");

        List<String> duplicates = ToolUtils.getDuplicateToolNames(callback1, callback2, callback3);

        assertThat(duplicates).isEmpty();
    }

    static class TestToolCallback implements ToolCallback {

        private final String name;

        private final String description;

        private final String inputTypeSchema;

        public TestToolCallback(String name) {
            this.name = name;
            this.description = "";
            this.inputTypeSchema = "";
        }

        public TestToolCallback(String name, String description, String inputTypeSchema) {
            this.name = name;
            this.description = description;
            this.inputTypeSchema = inputTypeSchema;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public String getInputTypeSchema() {
            return inputTypeSchema;
        }

        @Override
        public String call(String functionInput) {
            return "";
        }

    }

}
