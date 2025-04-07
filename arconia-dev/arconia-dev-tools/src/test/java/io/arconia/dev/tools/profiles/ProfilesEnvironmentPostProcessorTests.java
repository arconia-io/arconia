package io.arconia.dev.tools.profiles;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProfilesEnvironmentPostProcessor}.
 */
class ProfilesEnvironmentPostProcessorTests {

    private final ProfilesEnvironmentPostProcessor processor = new ProfilesEnvironmentPostProcessor();

    @Test
    void postProcessEnvironmentShouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> processor.postProcessEnvironment(null, new SpringApplication()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("environment cannot be null");
    }

    @Test
    void postProcessEnvironmentShouldNotAddProfilesWhenDisabled() {
        var environment = new MockEnvironment().withProperty("arconia.dev.profiles.enabled", "false");
        var application = new SpringApplication();

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getActiveProfiles()).isEmpty();
    }

    @Test
    void postProcessEnvironmentShouldAddDevProfileInNonTestEnvironment() {
        new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(Test.class))
            .withInitializer(context -> {
                var application = new SpringApplication(TestConfig.class);
                application.setMainApplicationClass(context.getClass());
                processor.postProcessEnvironment(context.getEnvironment(), application);
            })
            .run(context -> {
                assertThat(context.getEnvironment().getActiveProfiles()).contains("dev");
            });
    }

    @Test
    void postProcessEnvironmentShouldAddTestProfileInTestEnvironment() {
        var environment = new MockEnvironment();
        var application = new SpringApplication();

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getActiveProfiles()).contains("test");
    }

    @Test
    void postProcessEnvironmentShouldAddCustomDevProfile() {
        new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(Test.class))
            .withPropertyValues("arconia.dev.profiles.development=custom-dev")
            .withInitializer(context -> {
                var application = new SpringApplication(TestConfig.class);
                application.setMainApplicationClass(context.getClass());
                processor.postProcessEnvironment(context.getEnvironment(), application);
            })
            .run(context -> {
                assertThat(context.getEnvironment().getActiveProfiles()).contains("custom-dev");
            });
    }

    @Test
    void postProcessEnvironmentShouldAddCustomTestProfile() {
        var environment = new MockEnvironment().withProperty("arconia.dev.profiles.test", "custom-test");
        var application = new SpringApplication();

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getActiveProfiles()).contains("custom-test");
    }

    @Test
    void postProcessEnvironmentShouldNotAddEmptyDevProfile() {
        new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(Test.class))
            .withPropertyValues("arconia.dev.profiles.development=")
            .withInitializer(context -> {
                var application = new SpringApplication(TestConfig.class);
                application.setMainApplicationClass(context.getClass());
                processor.postProcessEnvironment(context.getEnvironment(), application);
            })
            .run(context -> {
                assertThat(context.getEnvironment().getActiveProfiles()).doesNotContain("dev");
            });
    }

    @Test
    void postProcessEnvironmentShouldNotAddEmptyTestProfile() {
        var environment = new MockEnvironment().withProperty("arconia.dev.profiles.test", "");
        var application = new SpringApplication();

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getActiveProfiles()).doesNotContain("test");
    }

    @Test
    void postProcessEnvironmentShouldAddMultipleDevProfiles() {
        new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(Test.class))
            .withPropertyValues("arconia.dev.profiles.development=dev1,dev2,dev3")
            .withInitializer(context -> {
                var application = new SpringApplication(TestConfig.class);
                application.setMainApplicationClass(context.getClass());
                processor.postProcessEnvironment(context.getEnvironment(), application);
            })
            .run(context -> {
                assertThat(context.getEnvironment().getActiveProfiles())
                    .contains("dev1", "dev2", "dev3");
            });
    }

    @Test
    void postProcessEnvironmentShouldAddMultipleTestProfiles() {
        var environment = new MockEnvironment()
            .withProperty("arconia.dev.profiles.test", "test1, \ntest2,test3");
        var application = new SpringApplication();

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getActiveProfiles())
            .contains("test1", "test2", "test3");
    }

    @Test
    void postProcessEnvironmentShouldFilterEmptyProfilesInList() {
        new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(Test.class))
            .withPropertyValues("arconia.dev.profiles.development=dev1,,dev2, ,dev3")
            .withInitializer(context -> {
                var application = new SpringApplication(TestConfig.class);
                application.setMainApplicationClass(context.getClass());
                processor.postProcessEnvironment(context.getEnvironment(), application);
            })
            .run(context -> {
                assertThat(context.getEnvironment().getActiveProfiles())
                    .contains("dev1", "dev2", "dev3")
                    .hasSize(3);
            });
    }

    @Configuration
    static class TestConfig {
    }

}
