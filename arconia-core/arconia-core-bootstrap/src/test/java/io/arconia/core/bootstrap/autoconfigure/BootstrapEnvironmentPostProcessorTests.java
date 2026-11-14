package io.arconia.core.bootstrap.autoconfigure;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.AbstractAotProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import io.arconia.core.bootstrap.BootstrapConfigurationFile;
import io.arconia.core.bootstrap.BootstrapMode;
import io.arconia.core.bootstrap.BootstrapModeDetector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link BootstrapEnvironmentPostProcessor}.
 */
@ExtendWith(OutputCaptureExtension.class)
@TestMethodOrder(MethodOrderer.Random.class)
class BootstrapEnvironmentPostProcessorTests {

    private final BootstrapEnvironmentPostProcessor processor = new BootstrapEnvironmentPostProcessor(Supplier::get);

    @BeforeEach
    void setUp() {
        BootstrapModeDetector.clearCache();
    }

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
        var environment = new MockEnvironment().withProperty("arconia.bootstrap.profiles.enabled", "false");
        var application = new SpringApplication();

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getActiveProfiles()).isEmpty();
    }

    @Test
    void shouldLogBootstrapModeDuringApplicationStartup(CapturedOutput output) {
        // Runs the full startup, where Spring Boot suppresses any logging until the logging system is initialized.
        var application = new SpringApplication(TestConfig.class);
        application.setWebApplicationType(WebApplicationType.NONE);

        application.run().close();

        assertThat(output).contains("The application is running in test mode");
    }

    @Test
    void shouldWarnWhenBootstrapModeIsInvalid(CapturedOutput output) {
        System.setProperty(BootstrapMode.SYSTEM_PROPERTY, "invalid");
        try {
            processor.postProcessEnvironment(new MockEnvironment(), new SpringApplication());
        } finally {
            System.clearProperty(BootstrapMode.SYSTEM_PROPERTY);
        }

        assertThat(output).contains("Invalid arconia.bootstrap.mode property value: 'invalid'. Defaulting to PROD mode.");
    }

    @Test
    void shouldNotWarnWhenBootstrapModeIsTestDuringAotProcessing(CapturedOutput output) {
        // The mode detected when Spring AOT processes the tests.
        System.setProperty(AbstractAotProcessor.AOT_PROCESSING, "true");
        try {
            processor.postProcessEnvironment(new MockEnvironment(), new SpringApplication());
        } finally {
            System.clearProperty(AbstractAotProcessor.AOT_PROCESSING);
        }

        assertThat(output).contains("The application is running in test mode");
        assertThat(output).doesNotContain("while Spring AOT processing is running");
    }

    @Test
    void shouldWarnWhenBootstrapModeIsDevDuringAotProcessing(CapturedOutput output) {
        System.setProperty(BootstrapMode.SYSTEM_PROPERTY, "dev");
        System.setProperty(AbstractAotProcessor.AOT_PROCESSING, "true");
        try {
            processor.postProcessEnvironment(new MockEnvironment(), new SpringApplication());
        } finally {
            System.clearProperty(BootstrapMode.SYSTEM_PROPERTY);
            System.clearProperty(AbstractAotProcessor.AOT_PROCESSING);
        }

        assertThat(output).contains("The bootstrap mode is set to DEV while Spring AOT processing is running.");
    }

    // DEV

    @Test
    void shouldAddDefaultProfilesWhenDevMode() {
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
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
    void shouldAddCustomProfilesWhenDevMode() {
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.profiles=custom-dev")
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
    void shouldNotAddDuplicateProfilesWhenDevMode() {
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("spring.profiles.active=custom-dev")
                .withPropertyValues("arconia.dev.profiles=custom-dev")
                .withInitializer(context -> {
                    var application = new SpringApplication(TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(context.getEnvironment(), application);
                })
                .run(context -> {
                    assertThat(context.getEnvironment().getActiveProfiles()).hasSize(1);
                    assertThat(context.getEnvironment().getActiveProfiles()).contains("custom-dev");
                });
    }

    @Test
    void shouldNotAddEmptyProfileWhenDevMode() {
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.profiles=")
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
    void shouldAddMultipleProfilesWhenDevMode() {
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.profiles=dev1,dev2,dev3")
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
    void shouldAddIndexedProfilesWhenDevMode() {
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.profiles[0]=dev1", "arconia.dev.profiles[1]=dev2")
                .withInitializer(context -> {
                    var application = new SpringApplication(TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(context.getEnvironment(), application);
                })
                .run(context -> {
                    assertThat(context.getEnvironment().getActiveProfiles()).contains("dev1", "dev2");
                });
    }

    @Test
    void shouldNotAddDuplicateIndexedProfilesWhenDevMode() {
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("spring.profiles.active[0]=custom-dev")
                .withPropertyValues("arconia.dev.profiles=custom-dev")
                .withInitializer(context -> {
                    var application = new SpringApplication(TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(context.getEnvironment(), application);
                })
                .run(context -> {
                    // The indexed declaration is only visible through the Binder. Since the profile
                    // is recognized as already declared, the post-processor adds nothing on top.
                    assertThat(context.getEnvironment().getActiveProfiles()).isEmpty();
                });
    }

    @Test
    void shouldActivateProfilesFromCommandLineArguments() {
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withInitializer(context -> {
                    var environment = context.getEnvironment();
                    environment.getPropertySources().addFirst(new SimpleCommandLinePropertySource("--arconia.dev.profiles=cli-dev"));
                    var application = new SpringApplication(TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(environment, application);
                })
                .run(context -> {
                    assertThat(context.getEnvironment().getActiveProfiles()).contains("cli-dev");
                });
    }

    @Test
    void shouldPreferCommandLineArgumentsOverSystemProperties() {
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev", "arconia.dev.profiles=system-dev")
                .withInitializer(context -> {
                    var environment = context.getEnvironment();
                    environment.getPropertySources().addFirst(new SimpleCommandLinePropertySource("--arconia.dev.profiles=cli-dev"));
                    var application = new SpringApplication(TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(environment, application);
                })
                .run(context -> {
                    assertThat(context.getEnvironment().getActiveProfiles()).contains("cli-dev");
                    assertThat(context.getEnvironment().getActiveProfiles()).doesNotContain("system-dev");
                });
    }

    @Test
    void shouldActivateProfilesFromBootstrapConfigurationFile() {
        var classLoader = classLoaderWithBootstrapFile("arconia.dev.profiles=file-dev");
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withInitializer(context -> {
                    var application = new SpringApplication(new DefaultResourceLoader(classLoader), TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(context.getEnvironment(), application);
                })
                .run(context -> {
                    assertThat(context.getEnvironment().getActiveProfiles()).contains("file-dev");
                    assertThat(context.getEnvironment().getPropertySources()
                            .contains(BootstrapEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)).isTrue();
                });
    }

    @Test
    void shouldPreferSystemPropertiesOverBootstrapConfigurationFile() {
        var classLoader = classLoaderWithBootstrapFile("arconia.dev.profiles=file-dev");
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev", "arconia.dev.profiles=system-dev")
                .withInitializer(context -> {
                    var application = new SpringApplication(new DefaultResourceLoader(classLoader), TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(context.getEnvironment(), application);
                })
                .run(context -> {
                    assertThat(context.getEnvironment().getActiveProfiles()).contains("system-dev");
                    assertThat(context.getEnvironment().getActiveProfiles()).doesNotContain("file-dev");
                });
    }

    @Test
    void shouldIgnoreUnsupportedPropertiesInBootstrapConfigurationFile(CapturedOutput output) {
        var classLoader = classLoaderWithBootstrapFile("""
                arconia.dev.profiles[0]=file-dev
                arconia.bootstrap.mode=prod
                server.port=9090
                """);
        var environment = new MockEnvironment();
        System.setProperty(BootstrapMode.SYSTEM_PROPERTY, "dev");
        try {
            processor.postProcessEnvironment(environment, new SpringApplication(new DefaultResourceLoader(classLoader)));
        } finally {
            System.clearProperty(BootstrapMode.SYSTEM_PROPERTY);
        }

        assertThat(environment.getActiveProfiles()).containsExactly("file-dev");
        assertThat(environment.containsProperty("server.port")).isFalse();
        assertThat(environment.containsProperty("arconia.bootstrap.mode")).isFalse();
        assertThat(output)
                .contains("The 'server.port' property is not supported in the META-INF/arconia-bootstrap.properties file and is ignored.")
                .contains("The 'arconia.bootstrap.mode' property is not supported in the META-INF/arconia-bootstrap.properties file and is ignored.")
                .doesNotContain("The 'arconia.dev.profiles[0]' property is not supported");
    }

    @Test
    void shouldNotLoadBootstrapConfigurationFileWhenProductionMode(CapturedOutput output) {
        var classLoader = classLoaderWithBootstrapFile("""
                arconia.dev.profiles=file-dev
                server.port=9090
                """);
        var environment = new MockEnvironment();
        System.setProperty(BootstrapMode.SYSTEM_PROPERTY, "prod");
        try {
            processor.postProcessEnvironment(environment, new SpringApplication(new DefaultResourceLoader(classLoader)));
        } finally {
            System.clearProperty(BootstrapMode.SYSTEM_PROPERTY);
        }

        assertThat(environment.getPropertySources().contains(BootstrapEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)).isFalse();
        assertThat(output).doesNotContain("is not supported in the");
    }

    private static ClassLoader classLoaderWithBootstrapFile(String content) {
        return new ClassLoader(BootstrapEnvironmentPostProcessorTests.class.getClassLoader()) {
            @Override
            @Nullable
            public InputStream getResourceAsStream(String name) {
                if (BootstrapConfigurationFile.LOCATION.equals(name)) {
                    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
                }
                return super.getResourceAsStream(name);
            }
        };
    }

    @Test
    void shouldFilterEmptyProfilesInList() {
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.profiles=dev1,,dev2, ,dev3")
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

    // TEST

    @Test
    void shouldAddDefaultProfilesWhenTestMode() {
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=test")
                .withInitializer(context -> {
                    var application = new SpringApplication(TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(context.getEnvironment(), application);
                })
                .run(context -> {
                    assertThat(context.getEnvironment().getActiveProfiles()).contains("test");
                });
    }

    @Test
    void shouldAddCustomProfilesWhenTestMode() {
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=test")
                .withPropertyValues("arconia.test.profiles=custom-test")
                .withInitializer(context -> {
                    var application = new SpringApplication(TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(context.getEnvironment(), application);
                })
                .run(context -> {
                    assertThat(context.getEnvironment().getActiveProfiles()).contains("custom-test");
                });
    }

    @Test
    void shouldNotAddDuplicateProfilesWhenTestMode() {
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=test")
                .withPropertyValues("spring.profiles.active=custom-test")
                .withPropertyValues("arconia.test.profiles=custom-test")
                .withInitializer(context -> {
                    var application = new SpringApplication(TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(context.getEnvironment(), application);
                })
                .run(context -> {
                    assertThat(context.getEnvironment().getActiveProfiles()).hasSize(1);
                    assertThat(context.getEnvironment().getActiveProfiles()).contains("custom-test");
                });
    }

    @Test
    void shouldNotAddEmptyProfileWhenTestMode() {
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=test")
                .withPropertyValues("arconia.test.profiles=")
                .withInitializer(context -> {
                    var application = new SpringApplication(TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(context.getEnvironment(), application);
                })
                .run(context -> {
                    assertThat(context.getEnvironment().getActiveProfiles()).doesNotContain("test");
                });
    }

    @Test
    void shouldAddMultipleProfilesWhenTestMode() {
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=test")
                .withPropertyValues("arconia.test.profiles=test1, \ntest2,test3")
                .withInitializer(context -> {
                    var application = new SpringApplication(TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(context.getEnvironment(), application);
                })
                .run(context -> {
                    assertThat(context.getEnvironment().getActiveProfiles())
                            .contains("test1", "test2", "test3");
                });
    }

    // PRODUCTION

    @Test
    void shouldAddNoProfileWhenProductionMode() {
        new ApplicationContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=prod")
                .withInitializer(context -> {
                    var application = new SpringApplication(TestConfig.class);
                    application.setMainApplicationClass(context.getClass());
                    processor.postProcessEnvironment(context.getEnvironment(), application);
                })
                .run(context -> {
                    assertThat(context.getEnvironment().getActiveProfiles()).isEmpty();
                });
    }

    @Configuration
    static class TestConfig {
    }

}
