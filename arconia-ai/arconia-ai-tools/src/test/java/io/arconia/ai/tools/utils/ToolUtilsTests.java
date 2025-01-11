package io.arconia.ai.tools.utils;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.arconia.ai.tools.ToolCallback;
import io.arconia.ai.tools.annotation.Tool;
import io.arconia.ai.tools.definition.ToolDefinition;
import io.arconia.ai.tools.execution.DefaultToolCallResultConverter;
import io.arconia.ai.tools.execution.ToolCallResultConverter;
import io.arconia.ai.tools.execution.ToolExecutionMode;
import io.arconia.ai.tools.util.ToolUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void shouldGetToolNameFromAnnotation() throws Exception {
        Method method = TestTools.class.getMethod("toolWithCustomName");
        assertThat(ToolUtils.getToolName(method)).isEqualTo("customName");
    }

    @Test
    void shouldGetMethodNameWhenNoCustomNameInAnnotation() throws Exception {
        Method method = TestTools.class.getMethod("toolWithoutCustomName");
        assertThat(ToolUtils.getToolName(method)).isEqualTo("toolWithoutCustomName");
    }

    @Test
    void shouldGetMethodNameWhenNoAnnotation() throws Exception {
        Method method = TestTools.class.getMethod("methodWithoutAnnotation");
        assertThat(ToolUtils.getToolName(method)).isEqualTo("methodWithoutAnnotation");
    }

    @Test
    void shouldGetToolDescriptionFromAnnotation() throws Exception {
        Method method = TestTools.class.getMethod("toolWithCustomDescription");
        assertThat(ToolUtils.getToolDescription(method)).isEqualTo("Custom description");
    }

    @Test
    void shouldGetMethodNameWhenNoCustomDescriptionInAnnotation() throws Exception {
        Method method = TestTools.class.getMethod("toolWithoutCustomDescription");
        assertThat(ToolUtils.getToolDescription(method)).isEqualTo("toolWithoutCustomDescription");
    }

    @Test
    void shouldGetFormattedMethodNameWhenNoAnnotation() throws Exception {
        Method method = TestTools.class.getMethod("camelCaseMethodWithoutAnnotation");
        assertThat(ToolUtils.getToolDescription(method)).isEqualTo("camel case method without annotation");
    }

    @Test
    void shouldGetToolExecutionModeFromAnnotation() throws Exception {
        Method method = TestTools.class.getMethod("toolWithCustomExecutionMode");
        assertThat(ToolUtils.getToolExecutionMode(method)).isEqualTo(ToolExecutionMode.BLOCKING);
    }

    @Test
    void shouldGetDefaultExecutionModeWhenNoAnnotation() throws Exception {
        Method method = TestTools.class.getMethod("methodWithoutAnnotation");
        assertThat(ToolUtils.getToolExecutionMode(method)).isEqualTo(ToolExecutionMode.BLOCKING);
    }

    @Test
    void shouldGetToolReturnDirectFromAnnotation() throws Exception {
        Method method = TestTools.class.getMethod("toolWithReturnDirect");
        assertThat(ToolUtils.getToolReturnDirect(method)).isTrue();
    }

    @Test
    void shouldGetDefaultReturnDirectWhenNoAnnotation() throws Exception {
        Method method = TestTools.class.getMethod("methodWithoutAnnotation");
        assertThat(ToolUtils.getToolReturnDirect(method)).isFalse();
    }

    @Test
    void shouldGetToolCallResultConverterFromAnnotation() throws Exception {
        Method method = TestTools.class.getMethod("toolWithCustomConverter");
        ToolCallResultConverter converter = ToolUtils.getToolCallResultConverter(method);
        assertThat(converter).isInstanceOf(CustomToolCallResultConverter.class);
    }

    @Test
    void shouldGetDefaultConverterWhenNoAnnotation() throws Exception {
        Method method = TestTools.class.getMethod("methodWithoutAnnotation");
        ToolCallResultConverter converter = ToolUtils.getToolCallResultConverter(method);
        assertThat(converter).isInstanceOf(DefaultToolCallResultConverter.class);
    }

    @Test
    void shouldThrowExceptionWhenConverterCannotBeInstantiated() throws Exception {
        Method method = TestTools.class.getMethod("toolWithInvalidConverter");
        assertThatThrownBy(() -> ToolUtils.getToolCallResultConverter(method))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Failed to instantiate ToolCallResultConverter");
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

    static class TestTools {

        @Tool(name = "customName")
        public void toolWithCustomName() {}

        @Tool
        public void toolWithoutCustomName() {}

        @Tool(value = "Custom description")
        public void toolWithCustomDescription() {}

        @Tool
        public void toolWithoutCustomDescription() {}

        @Tool(executionMode = ToolExecutionMode.BLOCKING)
        public void toolWithCustomExecutionMode() {}

        @Tool(returnDirect = true)
        public void toolWithReturnDirect() {}

        @Tool(resultConverter = CustomToolCallResultConverter.class)
        public void toolWithCustomConverter() {}

        @Tool(resultConverter = InvalidToolCallResultConverter.class)
        public void toolWithInvalidConverter() {}

        public void methodWithoutAnnotation() {}

        public void camelCaseMethodWithoutAnnotation() {}

    }

    public static class CustomToolCallResultConverter implements ToolCallResultConverter {

        @Override
        public String apply(Object result, Class<?> returnType) {
            return returnType.getName();
        }

    }

    // No-public class with no-public constructor
    static class InvalidToolCallResultConverter implements ToolCallResultConverter {

        private InvalidToolCallResultConverter() {}

        @Override
        public String apply(Object result, Class<?> returnType) {
            return returnType.getName();
        }

    }
}
