package io.arconia.ai.core.tools.method;

import java.util.List;

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
    void shouldResolveToolCallbacks() {
        TestComponent testComponent = new TestComponent();
        MethodToolCallbackResolver resolver = MethodToolCallbackResolver.builder().target(testComponent).build();

        FunctionCallback[] callbacks = resolver.getToolCallbacks();

        assertThat(callbacks).hasSize(1);
        MethodInvokingFunctionCallback callback = (MethodInvokingFunctionCallback) callbacks[0];
        assertThat(callback.getName()).isEqualTo("testMethod");
        assertThat(callback.getDescription()).isEqualTo("Test description");
    }

    @Test
    void shouldFailWhenTargetIsNotProvided() {
        assertThatThrownBy(() -> MethodToolCallbackResolver.builder().build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("target cannot be null");
    }

    static class TestComponent {

        @Tool("Test description")
        public List<String> testMethod(String input) {
            return List.of(input);
        }

        public void nonToolMethod() {
            // This method should be ignored as it doesn't have @Tool annotation
        }

    }

}
