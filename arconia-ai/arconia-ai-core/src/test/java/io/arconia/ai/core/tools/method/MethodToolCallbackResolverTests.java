package io.arconia.ai.core.tools.method;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.MethodInvokingFunctionCallback;

import io.arconia.ai.core.tools.Tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MethodToolCallbackResolver}.
 */
class MethodToolCallbackResolverTests {

    @Test
    void shouldResolveToolCallbacksFromObject() {
        TestComponent testComponent = new TestComponent();
        MethodToolCallbackResolver resolver = MethodToolCallbackResolver.builder().object(testComponent).build();

        FunctionCallback[] callbacks = resolver.getToolCallbacks();

        assertThat(callbacks).hasSize(2);

        var callback1 = Stream.of(callbacks)
            .filter(c -> c.getName().equals("testMethod"))
            .map(m -> (MethodInvokingFunctionCallback) m)
            .findFirst();
        assertThat(callback1).isPresent();
        assertThat(callback1.get().getName()).isEqualTo("testMethod");
        assertThat(callback1.get().getDescription()).isEqualTo("Test description");

        var callback2 = Stream.of(callbacks)
            .filter(c -> c.getName().equals("testStaticMethod"))
            .map(m -> (MethodInvokingFunctionCallback) m)
            .findFirst();
        assertThat(callback2).isPresent();
        assertThat(callback2.get().getName()).isEqualTo("testStaticMethod");
        assertThat(callback2.get().getDescription()).isEqualTo("Test description");
    }

    @Test
    void shouldResolveToolCallbacksFromAllMethodsInType() {
        MethodToolCallbackResolver resolver = MethodToolCallbackResolver.builder().type(TestComponent.class).build();

        FunctionCallback[] callbacks = resolver.getToolCallbacks();

        assertThat(callbacks).hasSize(2);

        var callback1 = Stream.of(callbacks)
            .filter(c -> c.getName().equals("testMethod"))
            .map(m -> (MethodInvokingFunctionCallback) m)
            .findFirst();
        assertThat(callback1).isPresent();
        assertThat(callback1.get().getName()).isEqualTo("testMethod");
        assertThat(callback1.get().getDescription()).isEqualTo("Test description");

        var callback2 = Stream.of(callbacks)
            .filter(c -> c.getName().equals("testStaticMethod"))
            .map(m -> (MethodInvokingFunctionCallback) m)
            .findFirst();
        assertThat(callback2).isPresent();
        assertThat(callback2.get().getName()).isEqualTo("testStaticMethod");
        assertThat(callback2.get().getDescription()).isEqualTo("Test description");
    }

    @Test
    void shouldResolveToolCallbacksOnlyFromStaticMethodsInType() {
        MethodToolCallbackResolver resolver = MethodToolCallbackResolver.builder()
            .type(OtherTestComponent.class)
            .build();

        FunctionCallback[] callbacks = resolver.getToolCallbacks();

        assertThat(callbacks).hasSize(1);

        var callback2 = Stream.of(callbacks)
            .filter(c -> c.getName().equals("testStaticMethod"))
            .map(m -> (MethodInvokingFunctionCallback) m)
            .findFirst();
        assertThat(callback2).isPresent();
        assertThat(callback2.get().getName()).isEqualTo("testStaticMethod");
        assertThat(callback2.get().getDescription()).isEqualTo("Test description");
    }

    @Test
    void shouldFailWhenTargetTypeIsNotProvided() {
        assertThatThrownBy(() -> MethodToolCallbackResolver.builder().build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("targetType cannot be null");
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

}
