package io.arconia.ai.core.tools.method;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.arconia.ai.core.tools.ToolCallback;
import io.arconia.ai.core.tools.annotation.Tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MethodToolCallbackProvider}.
 */
class MethodToolCallbackProviderTests {

    @Test
    void shouldProvideToolCallbacksFromObject() {
        TestComponent testComponent = new TestComponent();
        MethodToolCallbackProvider provider = MethodToolCallbackProvider.builder().sources(testComponent).build();

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
    void shouldProvideToolCallbacksOnlyFromStaticMethodsInType() {
        MethodToolCallbackProvider provider = MethodToolCallbackProvider.builder()
            .sources(OtherTestComponent.class)
            .build();

        ToolCallback[] callbacks = provider.getToolCallbacks();

        assertThat(callbacks).hasSize(1);

        var callback2 = Stream.of(callbacks).filter(c -> c.getName().equals("testStaticMethod")).findFirst();
        assertThat(callback2).isPresent();
        assertThat(callback2.get().getName()).isEqualTo("testStaticMethod");
        assertThat(callback2.get().getDescription()).isEqualTo("Test description");
    }

    @Test
    void shouldEnsureUniqueToolNames() {
        TestComponentWithDuplicates testComponent = new TestComponentWithDuplicates();
        MethodToolCallbackProvider provider = MethodToolCallbackProvider.builder().sources(testComponent).build();

        assertThatThrownBy(provider::getToolCallbacks).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(
                    "Multiple tools with the same name found in sources: " + testComponent.getClass().getName());
    }

    static class TestComponent {

        @Tool("Test description")
        public static List<String> testStaticMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        public List<String> testMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        public Function<String, Integer> testFunction(String input) {
            // This method should be ignored as it's a functional type, which is not
            // supported.
            return String::length;
        }

        public void nonToolMethod() {
            // This method should be ignored as it doesn't have @Tool annotation
        }

    }

    static class OtherTestComponent {

        public OtherTestComponent(String something) {
            System.out.println(something);
        }

        @Tool("Test description")
        public static List<String> testStaticMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        public List<String> testMethod(String input) {
            return List.of(input);
        }

        public void nonToolMethod() {
            // This method should be ignored as it doesn't have @Tool annotation
        }

    }

    static class TestComponentWithDuplicates {

        @Tool(name = "testMethod", value = "Test description")
        public List<String> testMethod1(String input) {
            return List.of(input);
        }

        @Tool(name = "testMethod", value = "Test description")
        public List<String> testMethod2(String input) {
            return List.of(input);
        }

    }

}
