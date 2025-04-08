package io.arconia.dev.tools.profiles;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ProfilesApplicationListener}.
 */
class ProfilesApplicationListenerTests {

    private final ProfilesApplicationListener listener = new ProfilesApplicationListener();

    @Test
    void onApplicationEventShouldThrowExceptionWhenEventIsNull() {
        assertThatThrownBy(() -> listener.onApplicationEvent(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("event cannot be null");
    }

    @Test
    void onApplicationEventShouldNotAddProfilesWhenDisabled() {
        var environment = new MockEnvironment().withProperty("arconia.dev.profiles.enabled", "false");
        var application = new SpringApplication();
        var bootstrapContext = new DefaultBootstrapContext();
        var event = new ApplicationEnvironmentPreparedEvent(bootstrapContext, application, new String[0], environment);

        listener.onApplicationEvent(event);

        assertThat(environment.getActiveProfiles()).isEmpty();
    }

    @Test
    void onApplicationEventShouldAddDevProfileInNonTestEnvironment() {
        new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(Test.class))
            .withInitializer(context -> {
                var application = new SpringApplication(TestConfig.class);
                application.setMainApplicationClass(context.getClass());
                var bootstrapContext = new DefaultBootstrapContext();
                var event = new ApplicationEnvironmentPreparedEvent(bootstrapContext, application, new String[0], context.getEnvironment());
                listener.onApplicationEvent(event);
            })
            .run(context -> {
                assertThat(context.getEnvironment().getActiveProfiles()).contains("dev");
            });
    }

    @Test
    void onApplicationEventShouldAddTestProfileInTestEnvironment() {
        var environment = new MockEnvironment();
        var application = new SpringApplication();
        var bootstrapContext = new DefaultBootstrapContext();
        var event = new ApplicationEnvironmentPreparedEvent(bootstrapContext, application, new String[0], environment);

        listener.onApplicationEvent(event);

        assertThat(environment.getActiveProfiles()).contains("test");
    }

    @Test
    void onApplicationEventShouldAddCustomDevProfile() {
        new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(Test.class))
            .withPropertyValues("arconia.dev.profiles.development=custom-dev")
            .withInitializer(context -> {
                var application = new SpringApplication(TestConfig.class);
                application.setMainApplicationClass(context.getClass());
                var bootstrapContext = new DefaultBootstrapContext();
                var event = new ApplicationEnvironmentPreparedEvent(bootstrapContext, application, new String[0], context.getEnvironment());
                listener.onApplicationEvent(event);
            })
            .run(context -> {
                assertThat(context.getEnvironment().getActiveProfiles()).contains("custom-dev");
            });
    }

    @Test
    void onApplicationEventShouldAddCustomTestProfile() {
        var environment = new MockEnvironment().withProperty("arconia.dev.profiles.test", "custom-test");
        var application = new SpringApplication();
        var bootstrapContext = new DefaultBootstrapContext();
        var event = new ApplicationEnvironmentPreparedEvent(bootstrapContext, application, new String[0], environment);

        listener.onApplicationEvent(event);

        assertThat(environment.getActiveProfiles()).contains("custom-test");
    }

    @Test
    void onApplicationEventShouldNotAddEmptyDevProfile() {
        new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(Test.class))
            .withPropertyValues("arconia.dev.profiles.development=")
            .withInitializer(context -> {
                var application = new SpringApplication(TestConfig.class);
                application.setMainApplicationClass(context.getClass());
                var bootstrapContext = new DefaultBootstrapContext();
                var event = new ApplicationEnvironmentPreparedEvent(bootstrapContext, application, new String[0], context.getEnvironment());
                listener.onApplicationEvent(event);
            })
            .run(context -> {
                assertThat(context.getEnvironment().getActiveProfiles()).doesNotContain("dev");
            });
    }

    @Test
    void onApplicationEventShouldNotAddEmptyTestProfile() {
        var environment = new MockEnvironment().withProperty("arconia.dev.profiles.test", "");
        var application = new SpringApplication();
        var bootstrapContext = new DefaultBootstrapContext();
        var event = new ApplicationEnvironmentPreparedEvent(bootstrapContext, application, new String[0], environment);

        listener.onApplicationEvent(event);

        assertThat(environment.getActiveProfiles()).doesNotContain("test");
    }

    @Test
    void onApplicationEventShouldAddMultipleDevProfiles() {
        new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(Test.class))
            .withPropertyValues("arconia.dev.profiles.development=dev1,dev2,dev3")
            .withInitializer(context -> {
                var application = new SpringApplication(TestConfig.class);
                application.setMainApplicationClass(context.getClass());
                var bootstrapContext = new DefaultBootstrapContext();
                var event = new ApplicationEnvironmentPreparedEvent(bootstrapContext, application, new String[0], context.getEnvironment());
                listener.onApplicationEvent(event);
            })
            .run(context -> {
                assertThat(context.getEnvironment().getActiveProfiles())
                    .contains("dev1", "dev2", "dev3");
            });
    }

    @Test
    void onApplicationEventShouldAddMultipleTestProfiles() {
        var environment = new MockEnvironment()
            .withProperty("arconia.dev.profiles.test", "test1, \ntest2,test3");
        var application = new SpringApplication();
        var bootstrapContext = new DefaultBootstrapContext();
        var event = new ApplicationEnvironmentPreparedEvent(bootstrapContext, application, new String[0], environment);

        listener.onApplicationEvent(event);

        assertThat(environment.getActiveProfiles())
            .contains("test1", "test2", "test3");
    }

    @Test
    void onApplicationEventShouldFilterEmptyProfilesInList() {
        new ApplicationContextRunner()
            .withClassLoader(new FilteredClassLoader(Test.class))
            .withPropertyValues("arconia.dev.profiles.development=dev1,,dev2, ,dev3")
            .withInitializer(context -> {
                var application = new SpringApplication(TestConfig.class);
                application.setMainApplicationClass(context.getClass());
                var bootstrapContext = new DefaultBootstrapContext();
                var event = new ApplicationEnvironmentPreparedEvent(bootstrapContext, application, new String[0], context.getEnvironment());
                listener.onApplicationEvent(event);
            })
            .run(context -> {
                assertThat(context.getEnvironment().getActiveProfiles())
                    .contains("dev1", "dev2", "dev3")
                    .hasSize(3);
            });
    }

    @Test
    void shouldHaveExpectedOrder() {
        assertThat(listener.getOrder())
            .isEqualTo(org.springframework.boot.env.EnvironmentPostProcessorApplicationListener.DEFAULT_ORDER + 10);
    }

    @Configuration
    static class TestConfig {
    }

}
