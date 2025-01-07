package io.arconia.ai.tools.method;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.arconia.ai.tools.ToolCallback;
import io.arconia.ai.tools.annotation.Tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MethodToolCallbackProvider}.
 */
class MethodToolCallbackProviderTests {

    @Test
    void shouldProvideToolCallbacksFromObject() {
        Tools tools = new Tools();
        MethodToolCallbackProvider provider = MethodToolCallbackProvider.builder().toolObjects(tools).build();

        ToolCallback[] callbacks = provider.getToolCallbacks();

        assertThat(callbacks).hasSize(2);

        var callback1 = Stream.of(callbacks).filter(c -> c.getName().equals("testMethod")).findFirst();
        assertThat(callback1).isPresent();
        assertThat(callback1.get().getName()).isEqualTo("testMethod");
        assertThat(callback1.get().getDescription()).isEqualTo("Test description");

        var callback2 = Stream.of(callbacks).filter(c -> c.getName().equals("testStaticMethod")).findFirst();
        assertThat(callback2).isPresent();
        assertThat(callback2.get().getName()).isEqualTo("testStaticMethod");
        assertThat(callback2.get().getDescription()).isEqualTo("Test description");
    }

    @Test
    void shouldEnsureUniqueToolNames() {
        ToolsWithDuplicates testComponent = new ToolsWithDuplicates();
        MethodToolCallbackProvider provider = MethodToolCallbackProvider.builder().toolObjects(testComponent).build();

        assertThatThrownBy(provider::getToolCallbacks).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Multiple tools with the same name (testMethod) found in sources: "
                    + testComponent.getClass().getName());
    }

    static class Tools {

        @Tool("Test description")
        static List<String> testStaticMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        List<String> testMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        Function<String, Integer> testFunction(String input) {
            // This method should be ignored as it's a functional type, which is not
            // supported.
            return String::length;
        }

        void nonToolMethod() {
            // This method should be ignored as it doesn't have @Tool annotation
        }

    }

    static class ToolsWithDuplicates {

        @Tool(name = "testMethod", value = "Test description")
        List<String> testMethod1(String input) {
            return List.of(input);
        }

        @Tool(name = "testMethod", value = "Test description")
        List<String> testMethod2(String input) {
            return List.of(input);
        }

    }

}
