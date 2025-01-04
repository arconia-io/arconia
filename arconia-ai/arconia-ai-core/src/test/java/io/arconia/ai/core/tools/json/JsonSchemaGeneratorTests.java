package io.arconia.ai.core.tools.json;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.ReflectionUtils;

class JsonSchemaGeneratorTests {

    private static Method getMethod(String name) {
        return Arrays.stream(ReflectionUtils.getDeclaredMethods(TestClass.class))
            .filter(m -> m.getName().equals(name))
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

        public String noArgsMethod() {
            return "Hello";
        }

        public String oneArgMethod(String greeting) {
            return greeting;
        }

        public String oneArgMethodList(List<String> greetings) {
            return String.join(", ", greetings);
        }

    }

}
