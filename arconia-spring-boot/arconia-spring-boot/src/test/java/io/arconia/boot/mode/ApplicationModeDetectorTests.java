package io.arconia.boot.mode;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ApplicationModeDetector}.
 */
class ApplicationModeDetectorTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    // DEVELOPMENT

    @Test
    void developmentModeWhenDevToolsIsPresent() {
        contextRunner
                .run(context -> {
                    boolean isDevelopmentModeDetected = ApplicationModeDetector.isDevelopmentModeDetected(context.getClassLoader());
                    assertThat(isDevelopmentModeDetected).isTrue();
                });
    }

    @Test
    void noDevelopmentMode() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(org.springframework.boot.devtools.RemoteSpringApplication.class))
                .run(context -> {
                    boolean isDevelopmentModeDetected = ApplicationModeDetector.isDevelopmentModeDetected(context.getClassLoader());
                    assertThat(isDevelopmentModeDetected).isFalse();
                });
    }

    // TEST

    @Test
    void testModeWhenSpringBootTestIsPresent() {
        contextRunner
            .run(context -> {
                boolean isTestModeDetected = ApplicationModeDetector.isTestModeDetected(context.getClassLoader());
                assertThat(isTestModeDetected).isTrue();
            });
    }

    @Test
    void testModeWhenSpringTestIsPresent() {
        contextRunner
            .withClassLoader(new FilteredClassLoader(org.springframework.boot.test.context.SpringBootTest.class))
            .run(context -> {
                boolean isTestModeDetected = ApplicationModeDetector.isTestModeDetected(context.getClassLoader());
                assertThat(isTestModeDetected).isTrue();
            });
    }

    @Test
    void noTestMode() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(org.springframework.boot.test.context.SpringBootTest.class,
                        org.springframework.test.context.TestContext.class))
                .run(context -> {
                    boolean isTestModeDetected = ApplicationModeDetector.isTestModeDetected(context.getClassLoader());
                    assertThat(isTestModeDetected).isFalse();
                });
    }

}
