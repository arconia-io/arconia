package io.arconia.boot.env;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.devtools.RemoteSpringApplication;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ProfilesEnvironmentPostProcessor}.
 */
class ProfilesEnvironmentPostProcessorTests {

    private final ProfilesEnvironmentPostProcessor processor = new ProfilesEnvironmentPostProcessor();

    @Test
    void shouldThrowExceptionWhenEnvironmentIsNull() {
        assertThatThrownBy(() -> processor.postProcessEnvironment(null, new SpringApplication()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("environment cannot be null");
    }

    @Test
    void shouldThrowExceptionWhenApplicationIsNull() {
        assertThatThrownBy(() -> processor.postProcessEnvironment(mock(ConfigurableEnvironment.class), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("application cannot be null");
    }

    @Test
    void shouldNotAddProfilesWhenDisabled() {
        var environment = new MockEnvironment().withProperty("arconia.config.profiles.enabled", "false");
        var application = new SpringApplication();

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getActiveProfiles()).isEmpty();
    }

    // DEVELOPMENT

    @Test
    void shouldAddDefaultProfilesWhenDevelopmentMode() {
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
    void shouldAddCustomProfilesWhenDevelopmentMode() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader(Test.class))
                .withPropertyValues("arconia.config.profiles.development=custom-dev")
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
    void shouldNotAddEmptyProfileWhenDevelopmentMode() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader(Test.class))
                .withPropertyValues("arconia.config.profiles.development=")
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
    void shouldAddMultipleProfilesWhenDevelopmentMode() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader(Test.class))
                .withPropertyValues("arconia.config.profiles.development=dev1,dev2,dev3")
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

    // TEST

    @Test
    void shouldAddDefaultProfilesWhenTestMode() {
        var environment = new MockEnvironment();
        var application = new SpringApplication();

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getActiveProfiles()).contains("test");
    }

    @Test
    void shouldAddCustomProfilesWhenTestMode() {
        var environment = new MockEnvironment().withProperty("arconia.config.profiles.test", "custom-test");
        var application = new SpringApplication();

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getActiveProfiles()).contains("custom-test");
    }

    @Test
    void shouldNotAddEmptyProfileWhenTestMode() {
        var environment = new MockEnvironment().withProperty("arconia.config.profiles.test", "");
        var application = new SpringApplication();

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getActiveProfiles()).doesNotContain("test");
    }

    @Test
    void shouldAddMultipleProfilesWhenTestMode() {
        var environment = new MockEnvironment()
                .withProperty("arconia.config.profiles.test", "test1, \ntest2,test3");
        var application = new SpringApplication();

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getActiveProfiles())
                .contains("test1", "test2", "test3");
    }

    @Test
    void shouldFilterEmptyProfilesInList() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader(Test.class))
                .withPropertyValues("arconia.config.profiles.development=dev1,,dev2, ,dev3")
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

    // PRODUCTION

    @Test
    void shouldAddDefaultProfilesWhenProductionMode() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader(Test.class, RemoteSpringApplication.class))
                .withInitializer(context -> {
                    var application = new SpringApplication(TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(context.getEnvironment(), application);
                })
                .run(context -> {
                    assertThat(context.getEnvironment().getActiveProfiles()).contains("prod");
                });
    }

    @Test
    void shouldAddCustomProfilesWhenProductionMode() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader(Test.class, RemoteSpringApplication.class))
                .withPropertyValues("arconia.config.profiles.production=custom-prod")
                .withInitializer(context -> {
                    var application = new SpringApplication(TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(context.getEnvironment(), application);
                })
                .run(context -> {
                    assertThat(context.getEnvironment().getActiveProfiles()).contains("custom-prod");
                });
    }

    @Test
    void shouldNotAddEmptyProfileWhenProductionMode() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader(Test.class, RemoteSpringApplication.class))
                .withPropertyValues("arconia.config.profiles.production=")
                .withInitializer(context -> {
                    var application = new SpringApplication(TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(context.getEnvironment(), application);
                })
                .run(context -> {
                    assertThat(context.getEnvironment().getActiveProfiles()).doesNotContain("prod");
                });
    }

    @Test
    void shouldAddMultipleProfilesWhenProductionMode() {
        new ApplicationContextRunner()
                .withClassLoader(new FilteredClassLoader(Test.class, RemoteSpringApplication.class))
                .withPropertyValues("arconia.config.profiles.production=prod1,prod2,prod3")
                .withInitializer(context -> {
                    var application = new SpringApplication(TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(context.getEnvironment(), application);
                })
                .run(context -> {
                    assertThat(context.getEnvironment().getActiveProfiles())
                            .contains("prod1", "prod2", "prod3");
                });
    }

    @Configuration
    static class TestConfig {
    }

}
