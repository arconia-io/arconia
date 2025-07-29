package io.arconia.boot.bootstrap;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

/**
 * Unit tests for {@link BootstrapModeDetector}.
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.Random.class)
class BootstrapModeDetectorTests {

    @Nullable
    private MockedStatic<BootstrapModeDetector> mockedBootstrapModeDetector;
    @Nullable
    private String originalArconiaBootstrapModeProperty;

    @BeforeEach
    void setUp() {
        BootstrapModeDetector.clearCache();

        // Capture original system property
        originalArconiaBootstrapModeProperty = System.getProperty(BootstrapMode.PROPERTY_KEY);
        System.clearProperty(BootstrapMode.PROPERTY_KEY);
    }

    @AfterEach
    void tearDown() {
        // Restore original system property
        if (originalArconiaBootstrapModeProperty == null) {
            System.clearProperty(BootstrapMode.PROPERTY_KEY);
        } else {
            System.setProperty(BootstrapMode.PROPERTY_KEY, originalArconiaBootstrapModeProperty);
        }
        originalArconiaBootstrapModeProperty = null;

        // Clean up mocks
        if (mockedBootstrapModeDetector != null) {
            mockedBootstrapModeDetector.close();
        }

        BootstrapModeDetector.clearCache();
    }

    // ARCONIA

    @ParameterizedTest
    @ValueSource(strings = {"DEV", "TEST", "PROD"})
    void whenArconiaBootstrapModePropertyIsSet(String modeValue) {
        System.setProperty(BootstrapMode.PROPERTY_KEY, modeValue);
        assertThat(BootstrapModeDetector.detect()).isEqualTo(BootstrapMode.valueOf(modeValue));
    }

    @Test
    void whenArconiaBootstrapModePropertyIsSetToInvalidDefaultsToProduction() {
        System.setProperty(BootstrapMode.PROPERTY_KEY, "INVALID_MODE");
        assertThat(BootstrapModeDetector.detect()).isEqualTo(BootstrapMode.PROD);
    }

    @Test
    void whenEmptyStackTrace() {
        StackTraceElement[] emptyStackTrace = new StackTraceElement[] {};
        // The test is running in a JUnit context, so we expect TEST mode.
        assertThat(BootstrapModeDetector.detect(emptyStackTrace)).isEqualTo(BootstrapMode.TEST);
    }

    // TEST (StackTrace)

    @Test
    void testModeWhenJUnitInStackTrace() {
        StackTraceElement[] testStackTrace = new StackTraceElement[] {
            new StackTraceElement("org.junit.runners.ParentRunner", "run", "ParentRunner.java", 363),
            new StackTraceElement("org.junit.runner.JUnitCore", "run", "JUnitCore.java", 137)
        };
        assertThat(BootstrapModeDetector.detect(testStackTrace)).isEqualTo(BootstrapMode.TEST);
    }

    @Test
    void testModeWhenJUnitPlatformInStackTrace() {
        StackTraceElement[] testStackTrace = new StackTraceElement[] {
            new StackTraceElement("org.junit.platform.engine.support.hierarchical.ThrowableCollector", "execute", "ThrowableCollector.java", 73),
            new StackTraceElement("org.junit.platform.engine.support.hierarchical.NodeTestTask", "executeRecursively", "NodeTestTask.java", 125)
        };
        assertThat(BootstrapModeDetector.detect(testStackTrace)).isEqualTo(BootstrapMode.TEST);
    }

    @Test
    void testModeWhenSpringBootTestInStackTrace() {
        StackTraceElement[] testStackTrace = new StackTraceElement[] {
            new StackTraceElement("org.springframework.boot.test.context.SpringBootTestContextBootstrapper",
                    "buildTestContext", "SpringBootTestContextBootstrapper.java", 99)
        };
        assertThat(BootstrapModeDetector.detect(testStackTrace)).isEqualTo(BootstrapMode.TEST);
    }

    @Test
    void testModeWhenCucumberInStackTrace() {
        StackTraceElement[] testStackTrace = new StackTraceElement[] {
            new StackTraceElement("cucumber.runtime.Runtime", "run", "Runtime.java", 300)
        };
        assertThat(BootstrapModeDetector.detect(testStackTrace)).isEqualTo(BootstrapMode.TEST);
    }

    @Test
    void testModeWhenMultipleTestFrameworksInStackTrace() {
        StackTraceElement[] testStackTrace = new StackTraceElement[] {
            new StackTraceElement("com.example.MyClass", "myMethod", "MyClass.java", 42),
            new StackTraceElement("org.junit.runners.ParentRunner", "run", "ParentRunner.java", 363),
            new StackTraceElement("org.springframework.boot.test.context.SpringBootTestContextBootstrapper",
                    "buildTestContext", "SpringBootTestContextBootstrapper.java", 99)
        };
        assertThat(BootstrapModeDetector.detect(testStackTrace)).isEqualTo(BootstrapMode.TEST);
    }

    @Test
    void testModeOverridesNativeContextWhenTestFrameworkInStackTrace() {
        StackTraceElement[] testStackTrace = new StackTraceElement[] {
            new StackTraceElement("org.junit.runners.ParentRunner", "run", "ParentRunner.java", 363),
            new StackTraceElement("com.example.TestClass", "testMethod", "TestClass.java", 42)
        };

        mockNativeContext(true);
        assertThat(BootstrapModeDetector.detect(testStackTrace)).isEqualTo(BootstrapMode.TEST);
    }

    @Test
    void testModeOverridesDevelopmentContextWhenTestFrameworkInStackTrace() {
        StackTraceElement[] testStackTrace = new StackTraceElement[] {
            new StackTraceElement("org.springframework.boot.test.context.SpringBootTestContextBootstrapper",
                    "buildTestContext", "SpringBootTestContextBootstrapper.java", 99),
            new StackTraceElement("com.example.TestClass", "testMethod", "TestClass.java", 42)
        };

        mockDevelopmentContext(true);
        assertThat(BootstrapModeDetector.detect(testStackTrace)).isEqualTo(BootstrapMode.TEST);
    }

    @Test
    void nullStackTraceUsesCurrentThreadStackTrace() {
        // The test is running in a JUnit context, so we expect TEST mode.
        assertThat(BootstrapModeDetector.detect((StackTraceElement[]) null)).isEqualTo(BootstrapMode.TEST);
    }

    // PROD (StackTrace)

    @Test
    void prodModeWhenAotProcessorInStackTrace() {
        StackTraceElement[] testStackTrace = new StackTraceElement[] {
            new StackTraceElement("org.springframework.boot.SpringApplicationAotProcessor",
                    "process", "SpringApplicationAotProcessor.java", 107)
        };
        assertThat(BootstrapModeDetector.detect(testStackTrace)).isEqualTo(BootstrapMode.PROD);
    }

    @Test
    void prodModeWhenAotProcessorOverridesNativeAndDevelopmentContext() {
        StackTraceElement[] aotStackTrace = new StackTraceElement[] {
            new StackTraceElement("org.springframework.boot.SpringApplicationAotProcessor",
                    "process", "SpringApplicationAotProcessor.java", 107),
            new StackTraceElement("com.example.Application", "main", "Application.java", 10)
        };

        mockNativeContext(false);
        mockDevelopmentContext(true);
        assertThat(BootstrapModeDetector.detect(aotStackTrace)).isEqualTo(BootstrapMode.PROD);
    }

    // PROD (Native)

    @Test
    void prodModeWhenNativeContext() {
        // Create a clean stack trace without test framework classes
        StackTraceElement[] cleanStackTrace = new StackTraceElement[] {
            new StackTraceElement("com.example.Application", "main", "Application.java", 10),
            new StackTraceElement("java.lang.Thread", "run", "Thread.java", 748)
        };

        mockNativeContext(true);
        assertThat(BootstrapModeDetector.detect(cleanStackTrace)).isEqualTo(BootstrapMode.PROD);
    }

    // DEV

    @Test
    void devModeWhenDevToolsIsPresent() {
        StackTraceElement[] cleanStackTrace = new StackTraceElement[] {
                new StackTraceElement("com.example.Application", "main", "Application.java", 10),
                new StackTraceElement("java.lang.Thread", "run", "Thread.java", 748)
        };
        // DevTools is in the classpath, so we expect DEV mode here.
        assertThat(BootstrapModeDetector.detect(cleanStackTrace)).isEqualTo(BootstrapMode.DEV);
    }

    @Test
    void devModeWhenDevelopmentContext() {
        // Create a clean stack trace without test framework classes
        StackTraceElement[] cleanStackTrace = new StackTraceElement[] {
            new StackTraceElement("com.example.Application", "main", "Application.java", 10),
            new StackTraceElement("java.lang.Thread", "run", "Thread.java", 748)
        };

        mockDevelopmentContext(true);
        assertThat(BootstrapModeDetector.detect(cleanStackTrace)).isEqualTo(BootstrapMode.DEV);
    }

    // PROD

    @Test
    void prodModeWhenNeitherNativeNorDevelopmentContextWithCleanStackTrace() {
        StackTraceElement[] cleanStackTrace = new StackTraceElement[] {
            new StackTraceElement("com.example.Application", "main", "Application.java", 10),
            new StackTraceElement("java.lang.Thread", "run", "Thread.java", 748)
        };

        mockNativeContext(false);
        mockDevelopmentContext(false);
        assertThat(BootstrapModeDetector.detect(cleanStackTrace)).isEqualTo(BootstrapMode.PROD);
    }

    @Test
    void cleanStackTraceDefaultsToProdWithNativeContext() {
        StackTraceElement[] cleanStackTrace = new StackTraceElement[] {
                new StackTraceElement("com.example.Application", "main", "Application.java", 10),
                new StackTraceElement("java.lang.Thread", "run", "Thread.java", 748)
        };

        mockNativeContext(true);
        assertThat(BootstrapModeDetector.detect(cleanStackTrace)).isEqualTo(BootstrapMode.PROD);
    }

    @Test
    void cleanStackTraceDefaultsToDevWithDevelopmentContext() {
        StackTraceElement[] cleanStackTrace = new StackTraceElement[] {
                new StackTraceElement("com.example.Application", "main", "Application.java", 10),
                new StackTraceElement("java.lang.Thread", "run", "Thread.java", 748)
        };

        mockDevelopmentContext(true);
        assertThat(BootstrapModeDetector.detect(cleanStackTrace)).isEqualTo(BootstrapMode.DEV);
    }

    @Test
    void cleanStackTraceDefaultsToProdWithNoSpecialContext() {
        StackTraceElement[] cleanStackTrace = new StackTraceElement[] {
                new StackTraceElement("com.example.Application", "main", "Application.java", 10),
                new StackTraceElement("java.lang.Thread", "run", "Thread.java", 748)
        };

        mockNativeContext(false);
        mockDevelopmentContext(false);
        assertThat(BootstrapModeDetector.detect(cleanStackTrace)).isEqualTo(BootstrapMode.PROD);
    }

    // CACHE

    @Test
    void cacheIsUsedForSubsequentCalls() {
        // First call with specific stack trace
        StackTraceElement[] prodStackTrace = new StackTraceElement[] {
            new StackTraceElement("org.springframework.boot.SpringApplicationAotProcessor",
                    "process", "SpringApplicationAotProcessor.java", 107)
        };

        BootstrapMode firstResult = BootstrapModeDetector.detect(prodStackTrace);
        assertThat(firstResult).isEqualTo(BootstrapMode.PROD);

        // Second call with different stack trace should return cached result
        StackTraceElement[] testStackTrace = new StackTraceElement[] {
            new StackTraceElement("org.junit.runners.ParentRunner", "run", "ParentRunner.java", 363)
        };

        // It's PROD (cached value) instead of TEST.
        BootstrapMode secondResult = BootstrapModeDetector.detect(testStackTrace);
        assertThat(secondResult).isEqualTo(BootstrapMode.PROD);
    }

    /**
     * Helper method to mock isNativeContext() using reflection.
     */
    private void mockNativeContext(boolean isNative) {
        if (mockedBootstrapModeDetector != null) {
            mockedBootstrapModeDetector.close();
        }
        mockedBootstrapModeDetector = mockStatic(BootstrapModeDetector.class, Mockito.CALLS_REAL_METHODS);
        mockedBootstrapModeDetector.when(BootstrapModeDetector::isNativeContext).thenReturn(isNative);
    }

    /**
     * Helper method to mock isDevelopmentContext() using reflection.
     */
    private void mockDevelopmentContext(boolean isDevelopment) {
        if (mockedBootstrapModeDetector != null) {
            mockedBootstrapModeDetector.close();
        }
        mockedBootstrapModeDetector = mockStatic(BootstrapModeDetector.class, Mockito.CALLS_REAL_METHODS);
        mockedBootstrapModeDetector.when(BootstrapModeDetector::isDevelopmentContext).thenReturn(isDevelopment);
    }

}
