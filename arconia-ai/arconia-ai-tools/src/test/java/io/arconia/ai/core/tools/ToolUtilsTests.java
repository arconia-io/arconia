package io.arconia.ai.core.tools;

import java.util.List;

import io.arconia.ai.tools.ToolCallback;
import org.junit.jupiter.api.Test;

import io.arconia.ai.tools.definition.ToolDefinition;
import io.arconia.ai.tools.util.ToolUtils;

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

        private final ToolDefinition toolDefinition;

        public TestToolCallback(String name) {
            this.toolDefinition = ToolDefinition.builder()
                .name(name)
                .description(name)
                .inputTypeSchema("{}")
                .build();
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return toolDefinition;
        }

        @Override
        public String call(String functionInput) {
            return "";
        }

    }

}
