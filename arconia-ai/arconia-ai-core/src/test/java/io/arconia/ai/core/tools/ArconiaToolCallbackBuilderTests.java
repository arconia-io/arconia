package io.arconia.ai.core.tools;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ArconiaToolCallbackBuilder}. Implementation based on
 * DefaultFunctionCallbackBuilderTests.
 */
class ArconiaToolCallbackBuilderTests {

    // Function

    @Test
    void whenFunctionDescriptionIsNullThenThrow() {
        assertThatThrownBy(() -> ToolCallback.builder().function("functionName", input -> "output").description(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Description must not be empty");
    }

    @Test
    void whenFunctionDescriptionIsEmptyThenThrow() {
        assertThatThrownBy(() -> ToolCallback.builder().function("functionName", input -> "output").description(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Description must not be empty");
    }

    @Test
    void whenFunctionInputTypeSchemaIsNullThenThrow() {
        assertThatThrownBy(
                () -> ToolCallback.builder().function("functionName", input -> "output").inputTypeSchema(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("InputTypeSchema must not be empty");
    }

    @Test
    void whenFunctionInputTypeSchemaIsEmptyThenThrow() {
        assertThatThrownBy(() -> ToolCallback.builder().function("functionName", input -> "output").inputTypeSchema(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("InputTypeSchema must not be empty");
    }

    @Test
    void whenFunctionSchemaTypeIsNullThenThrow() {
        assertThatThrownBy(() -> ToolCallback.builder().function("functionName", input -> "output").schemaType(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SchemaType must not be null");
    }

    @Test
    void whenFunctionResponseConverterIsNullThenThrow() {
        assertThatThrownBy(
                () -> ToolCallback.builder().function("functionName", input -> "output").responseConverter(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ResponseConverter must not be null");
    }

    @Test
    void whenFunctionNameIsNullThenThrow2() {
        assertThatThrownBy(() -> ToolCallback.builder().function(null, (Function) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("name cannot be null or empty");
    }

    @Test
    void whenFunctionIsNullThenThrow() {
        assertThatThrownBy(() -> ToolCallback.builder().function("functionName", (Function) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("function cannot be null");
    }

    @Test
    void whenFunctionThenReturn() {
        FunctionCallback.FunctionInvokingSpec<?, ?> functionBuilder = ((ArconiaToolCallbackBuilder) ToolCallback
            .builder()).function("functionName", input -> "output");
        assertThat(functionBuilder).isNotNull();
    }

    @Test
    void whenFunctionWithNullInputTypeThenThrow() {
        assertThatThrownBy(() -> ToolCallback.builder().function("functionName", input -> "output").build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("inputType cannot be null");
    }

    @Test
    void whenFunctionWithInputTypeThenReturn() {
        FunctionCallback functionCallback = ToolCallback.builder()
            .function("functionName", input -> "output")
            .description("description")
            .inputType(String.class)
            .build();
        assertThat(functionCallback).isNotNull();
        assertThat(functionCallback.getDescription()).isEqualTo("description");
        assertThat(functionCallback.getName()).isEqualTo("functionName");
        assertThat(functionCallback.getInputTypeSchema()).isNotEmpty();
    }

    @Test
    void whenFunctionWithGeneratedDescriptionThenReturn() {
        FunctionCallback functionCallback = ToolCallback.builder()
            .function("veryLongDescriptiveFunctionName", input -> "output")
            .inputType(String.class)
            .build();
        assertThat(functionCallback.getDescription()).isEqualTo("very long descriptive function name");
        assertThat(functionCallback.getName()).isEqualTo("veryLongDescriptiveFunctionName");
    }

    @Test
    void whenFunctionWithGenericInputTypeThenReturn() {
        FunctionCallback functionCallback = ToolCallback.builder()
            .function("functionName", input -> "output")
            .inputType(new ParameterizedTypeReference<GenericsRequest<Request>>() {
            })
            .build();
        assertThat(functionCallback.getName()).isEqualTo("functionName");
        assertThat(functionCallback.getInputTypeSchema()).isEqualTo("""
                {
                  "$schema" : "https://json-schema.org/draft/2020-12/schema",
                  "type" : "object",
                  "properties" : {
                    "datum" : {
                      "type" : "object",
                      "properties" : {
                        "value" : {
                          "type" : "string"
                        }
                      }
                    }
                  }
                }""");
    }

    // BiFunction

    @Test
    void whenBiFunctionNameIsNullThenThrow2() {
        assertThatThrownBy(() -> ToolCallback.builder().function(null, (BiFunction) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("name cannot be null or empty");
    }

    @Test
    void whenBiFunctionIsNullThenThrow() {
        assertThatThrownBy(() -> ToolCallback.builder().function("functionName", (BiFunction) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("biFunction cannot be null");
    }

    @Test
    void whenBiFunctionThenReturn() {
        FunctionCallback.FunctionInvokingSpec<?, ?> functionBuilder = ((ArconiaToolCallbackBuilder) ToolCallback
            .builder()).function("functionName", (input, context) -> "output");
        assertThat(functionBuilder).isNotNull();
    }

    // Method

    @Test
    void whenMethodDescriptionIsNullThenThrow() {
        assertThatThrownBy(() -> ToolCallback.builder().method(getTestMethod()).description(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Description must not be empty");
    }

    @Test
    void whenMethodDescriptionIsEmptyThenThrow() {
        assertThatThrownBy(() -> ToolCallback.builder().method(getTestMethod()).description(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Description must not be empty");
    }

    @Test
    void whenMethodInputTypeSchemaIsNullThenThrow() {
        assertThatThrownBy(() -> ToolCallback.builder().method(getTestMethod()).inputTypeSchema(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("InputTypeSchema must not be empty");
    }

    @Test
    void whenMethodInputTypeSchemaIsEmptyThenThrow() {
        assertThatThrownBy(() -> ToolCallback.builder().method(getTestMethod()).inputTypeSchema(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("InputTypeSchema must not be empty");
    }

    @Test
    void whenMethodSchemaTypeIsNullThenThrow() {
        assertThatThrownBy(() -> ToolCallback.builder().method(getTestMethod()).schemaType(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("SchemaType must not be null");
    }

    @Test
    void whenMethodResponseConverterIsNullThenThrow() {
        assertThatThrownBy(() -> ToolCallback.builder().method(getTestMethod()).responseConverter(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ResponseConverter must not be null");
    }

    @Test
    void whenMethodIsNullThenThrow() {
        assertThatThrownBy(() -> ToolCallback.builder().method(null)).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("method cannot be null");
    }

    @Test
    void whenMethodNameIsNullThenThrow() {
        assertThatThrownBy(() -> ToolCallback.builder().method(mock(Method.class)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("method name cannot be null or empty");
    }

    @Test
    void whenMethodThenReturn() {
        FunctionCallback.MethodInvokingSpec methodInvokeBuilder = ToolCallback.builder().method(getTestMethod());
        assertThat(methodInvokeBuilder).isNotNull();
    }

    @Test
    void whenMethodWithMissingTargetObjectThenThrow() {
        var method = getTestMethod();
        assertThatThrownBy(() -> ToolCallback.builder().method(method).targetClass(TestClass.class).build())
            .isInstanceOf(IllegalStateException.class)
            .hasRootCauseMessage("Function object must be provided for non-static methods!");
    }

    @Test
    void whenMethodAndNameIsNullThenThrow() {
        assertThatThrownBy(
                () -> ToolCallback.builder().method(getTestMethod()).targetClass(TestClass.class).name(null).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("name cannot be null or empty");
    }

    @Test
    void whenMethodAndTargetClassThenReturn() {
        var method = getStaticTestMethod();
        var functionCallback = ToolCallback.builder().method(method).targetClass(TestClass.class).build();
        assertThat(functionCallback).isNotNull();
    }

    @Test
    void whenMethodAndTargetObjectThenReturn() {
        var method = getTestMethod();
        var functionCallback = ToolCallback.builder().method(method).targetObject(new TestClass()).build();
        assertThat(functionCallback).isNotNull();
    }

    private static Method getTestMethod() {
        return Arrays.stream(ReflectionUtils.getDeclaredMethods(TestClass.class))
            .filter(m -> m.getName().equals("methodName"))
            .findFirst()
            .orElseThrow();
    }

    private static Method getStaticTestMethod() {
        return Arrays.stream(ReflectionUtils.getDeclaredMethods(TestClass.class))
            .filter(m -> m.getName().equals("staticMethodName"))
            .findFirst()
            .orElseThrow();
    }

    public static class TestClass {

        public static String staticMethodName(String arg1, Integer arg2) {
            return arg1 + arg2;
        }

        public String methodName(String arg1, Integer arg2) {
            return arg1 + arg2;
        }

    }

    public record Request(String value) {
    }

    public static class GenericsRequest<T> {

        private T datum;

        public T getDatum() {
            return this.datum;
        }

        public void setDatum(T value) {
            this.datum = value;
        }

    }

}
