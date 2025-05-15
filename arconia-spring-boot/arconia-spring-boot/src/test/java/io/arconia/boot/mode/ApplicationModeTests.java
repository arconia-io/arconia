package io.arconia.boot.mode;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.TestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ApplicationMode}.
 */
class ApplicationModeTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void testModeWhenSpringBootTestIsPresent() {
        contextRunner
                .run(context -> {
                    ApplicationMode mode = ApplicationMode.of(context);
                    assertThat(mode).isEqualTo(ApplicationMode.TEST);
                });
    }

    @Test
    void testModeWhenSpringTestIsPresent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(org.springframework.boot.test.context.SpringBootTest.class))
                .run(context -> {
                    ApplicationMode mode = ApplicationMode.of(context);
                    assertThat(mode).isEqualTo(ApplicationMode.TEST);
                });
    }

    @Test
    void developmentModeWhenDevToolsIsPresent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(TestContext.class, org.springframework.boot.test.context.SpringBootTest.class))
                .run(context -> {
                    ApplicationMode mode = ApplicationMode.of(context);
                    assertThat(mode).isEqualTo(ApplicationMode.DEVELOPMENT);
                });
    }

    @Test
    void productionModeAsFallback() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(
                        TestContext.class,
                        org.springframework.boot.test.context.SpringBootTest.class,
                        org.springframework.boot.devtools.RemoteSpringApplication.class))
                .run(context -> {
                    ApplicationMode mode = ApplicationMode.of(context);
                    assertThat(mode).isEqualTo(ApplicationMode.PRODUCTION);
                });
    }

}
