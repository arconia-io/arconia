package io.arconia.ai.tools.method;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.util.ReflectionUtils;

import io.arconia.ai.tools.annotation.Tool;
import io.arconia.ai.tools.definition.ToolDefinition;
import io.arconia.ai.tools.execution.ToolExecutionException;
import io.arconia.ai.tools.json.JsonParser;
import io.arconia.ai.tools.metadata.ToolMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MethodToolCallback}.
 */
class MethodToolCallbackTests {

    @ParameterizedTest
    @ValueSource(strings = {
            "publicStaticMethod",
            "privateStaticMethod",
            "packageStaticMethod",
            "publicMethod",
            "privateMethod",
            "packageMethod"
    })
    void shouldCallToolFromPublicClass(String methodName) {
        validateAssertions(methodName, new PublicTools());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "publicStaticMethod",
            "privateStaticMethod",
            "packageStaticMethod",
            "publicMethod",
            "privateMethod",
            "packageMethod"
    })
    void shouldCallToolFromPrivateClass(String methodName) {
        validateAssertions(methodName, new PrivateTools());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "publicStaticMethod",
            "privateStaticMethod",
            "packageStaticMethod",
            "publicMethod",
            "privateMethod",
            "packageMethod"
    })
    void shouldCallToolFromPackageClass(String methodName) {
        validateAssertions(methodName, new PackageTools());
    }

    @Test
    void shouldHandleToolContextWhenSupported() {
        Method toolMethod = getMethod("methodWithToolContext", ToolContextTools.class);
        MethodToolCallback callback = MethodToolCallback.builder()
                .toolDefinition(ToolDefinition.from(toolMethod))
                .toolMetadata(ToolMetadata.from(toolMethod))
                .toolMethod(toolMethod)
                .toolObject(new ToolContextTools())
                .build();

        ToolContext toolContext = new ToolContext(Map.of("key", "value"));
        String result = callback.call("""
                {
                    "input": "test"
                }
                """, toolContext);

        assertThat(result).contains("value");
    }

    @Test
    void shouldThrowExceptionWhenToolContextNotSupported() {
        Method toolMethod = getMethod("publicMethod", PublicTools.class);
        MethodToolCallback callback = MethodToolCallback.builder()
                .toolDefinition(ToolDefinition.from(toolMethod))
                .toolMetadata(ToolMetadata.from(toolMethod))
                .toolMethod(toolMethod)
                .toolObject(new PublicTools())
                .build();

        ToolContext toolContext = new ToolContext(Map.of("key", "value"));

        assertThatThrownBy(() -> callback.call("""
                {
                    "input": "test"
                }
                """, toolContext))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ToolContext is not supported");
    }

    @Test
    void shouldHandleComplexArguments() {
        Method toolMethod = getMethod("complexArgumentMethod", ComplexTools.class);
        MethodToolCallback callback = MethodToolCallback.builder()
                .toolDefinition(ToolDefinition.from(toolMethod))
                .toolMetadata(ToolMetadata.from(toolMethod))
                .toolMethod(toolMethod)
                .toolObject(new ComplexTools())
                .build();

        String result = callback.call("""
                {
                    "stringArg": "test",
                    "intArg": 42,
                    "listArg": ["a", "b", "c"],
                    "optionalArg": null
                }
                """);

        assertThat(JsonParser.fromJson(result, new TypeReference<Map<String, Object>>() {}))
                .containsEntry("stringValue", "test")
                .containsEntry("intValue", 42)
                .containsEntry("listSize", 3);
    }

    @Test
    void shouldHandleCustomResultConverter() {
        Method toolMethod = getMethod("publicMethod", PublicTools.class);
        MethodToolCallback callback = MethodToolCallback.builder()
                .toolDefinition(ToolDefinition.from(toolMethod))
                .toolMetadata(ToolMetadata.from(toolMethod))
                .toolMethod(toolMethod)
                .toolObject(new PublicTools())
                .toolCallResultConverter((result, type) -> "Converted: " + result)
                .build();

        String result = callback.call("""
                {
                    "input": "test"
                }
                """);

        assertThat(result).startsWith("Converted:");
    }

    @Test
    void shouldThrowExceptionWhenToolExecutionFails() {
        Method toolMethod = getMethod("errorMethod", ErrorTools.class);
        MethodToolCallback callback = MethodToolCallback.builder()
                .toolDefinition(ToolDefinition.from(toolMethod))
                .toolMetadata(ToolMetadata.from(toolMethod))
                .toolMethod(toolMethod)
                .toolObject(new ErrorTools())
                .build();

        assertThatThrownBy(() -> callback.call("""
                {
                    "input": "test"
                }
                """))
                .isInstanceOf(ToolExecutionException.class)
                .hasMessageContaining("Test error");
    }

    private static void validateAssertions(String methodName, Object toolObject) {
        Method toolMethod = getMethod(methodName, toolObject.getClass());
        assertThat(toolMethod).isNotNull();
        MethodToolCallback callback = MethodToolCallback.builder()
                .toolDefinition(ToolDefinition.from(toolMethod))
                .toolMetadata(ToolMetadata.from(toolMethod))
                .toolMethod(toolMethod)
                .toolObject(toolObject)
                .build();

        String result = callback.call("""
                {
                    "input": "Wingardium Leviosa"
                }
                """);

        assertThat(JsonParser.fromJson(result, new TypeReference<List<String>>() {}))
                .contains("Wingardium Leviosa");
    }

    private static Method getMethod(String name, Class<?> toolsClass) {
        return Arrays.stream(ReflectionUtils.getDeclaredMethods(toolsClass))
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    static public class PublicTools {

        @Tool("Test description")
        public static List<String> publicStaticMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        private static List<String> privateStaticMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        static List<String> packageStaticMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        public List<String> publicMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        private List<String> privateMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        List<String> packageMethod(String input) {
            return List.of(input);
        }

    }

    static private class PrivateTools {

        @Tool("Test description")
        public static List<String> publicStaticMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        private static List<String> privateStaticMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        static List<String> packageStaticMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        public List<String> publicMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        private List<String> privateMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        List<String> packageMethod(String input) {
            return List.of(input);
        }

    }

    static class PackageTools {

        @Tool("Test description")
        public static List<String> publicStaticMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        private static List<String> privateStaticMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        static List<String> packageStaticMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        public List<String> publicMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        private List<String> privateMethod(String input) {
            return List.of(input);
        }

        @Tool("Test description")
        List<String> packageMethod(String input) {
            return List.of(input);
        }

    }

    static class ToolContextTools {

        @Tool("Test description")
        public String methodWithToolContext(String input, ToolContext toolContext) {
            return input + ": " + toolContext.getContext().get("key");
        }

    }

    static class ComplexTools {

        @Tool("Test description")
        public Map<String, Object> complexArgumentMethod(String stringArg, int intArg, List<String> listArg, String optionalArg) {
            return Map.of(
                "stringValue", stringArg,
                "intValue", intArg,
                "listSize", listArg.size(),
                "optionalProvided", optionalArg != null
            );
        }

    }

    static class ErrorTools {

        @Tool("Test description")
        public String errorMethod(String input) {
            throw new IllegalArgumentException("Test error");
        }

    }

}
