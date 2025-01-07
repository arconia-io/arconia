package io.arconia.ai.tools.method;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.ReflectionUtils;

import io.arconia.ai.tools.annotation.Tool;
import io.arconia.ai.tools.definition.ToolDefinition;
import io.arconia.ai.tools.json.JsonParser;
import io.arconia.ai.tools.metadata.ToolMetadata;

import static org.assertj.core.api.Assertions.assertThat;

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

}
