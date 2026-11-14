package io.arconia.core.bootstrap.autoconfigure;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.AbstractAotProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;

import io.arconia.core.bootstrap.BootstrapConfigurationFile;
import io.arconia.core.bootstrap.BootstrapMode;

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

    // BOOTSTRAP MODE

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
        postProcess("invalid", new MockEnvironment());

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
        System.setProperty(AbstractAotProcessor.AOT_PROCESSING, "true");
        try {
            postProcess("dev", new MockEnvironment());
        } finally {
            System.clearProperty(AbstractAotProcessor.AOT_PROCESSING);
        }

        assertThat(output).contains("The bootstrap mode is set to DEV while Spring AOT processing is running.");
    }

    // PROFILES

    @ParameterizedTest
    @ValueSource(strings = { "dev", "test" })
    void shouldAddDefaultProfile(String mode) {
        var environment = postProcess(mode, new MockEnvironment());

        assertThat(environment.getActiveProfiles()).containsExactly(mode);
    }

    @ParameterizedTest
    @ValueSource(strings = { "dev", "test" })
    void shouldAddCustomProfile(String mode) {
        var environment = postProcess(mode, new MockEnvironment()
                .withProperty(profilesProperty(mode), "custom"));

        assertThat(environment.getActiveProfiles()).containsExactly("custom");
    }

    @ParameterizedTest
    @ValueSource(strings = { "dev", "test" })
    void shouldAddMultipleProfilesInOrder(String mode) {
        var environment = postProcess(mode, new MockEnvironment()
                .withProperty(profilesProperty(mode), "custom1, \ncustom2,custom3"));

        assertThat(environment.getActiveProfiles()).containsExactly("custom1", "custom2", "custom3");
    }

    @ParameterizedTest
    @ValueSource(strings = { "dev", "test" })
    void shouldAddIndexedProfiles(String mode) {
        var environment = postProcess(mode, new MockEnvironment()
                .withProperty(profilesProperty(mode) + "[0]", "custom1")
                .withProperty(profilesProperty(mode) + "[1]", "custom2"));

        assertThat(environment.getActiveProfiles()).containsExactly("custom1", "custom2");
    }

    @ParameterizedTest
    @ValueSource(strings = { "dev", "test" })
    void shouldFilterEmptyProfilesInList(String mode) {
        var environment = postProcess(mode, new MockEnvironment()
                .withProperty(profilesProperty(mode), "custom1,,custom2, ,custom3"));

        assertThat(environment.getActiveProfiles()).containsExactly("custom1", "custom2", "custom3");
    }

    @ParameterizedTest
    @ValueSource(strings = { "dev", "test" })
    void shouldNotAddProfileWhenEmpty(String mode) {
        var environment = postProcess(mode, new MockEnvironment()
                .withProperty(profilesProperty(mode), ""));

        assertThat(environment.getActiveProfiles()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "dev", "test" })
    void shouldNotAddProfilesWhenDisabled(String mode) {
        var environment = postProcess(mode, new MockEnvironment()
                .withProperty("arconia.bootstrap.profiles.enabled", "false"));

        assertThat(environment.getActiveProfiles()).isEmpty();
    }

    @Test
    void shouldOnlyAddProfilesOfCurrentMode() {
        var environment = postProcess("dev", new MockEnvironment()
                .withProperty("arconia.test.profiles", "custom"));

        assertThat(environment.getActiveProfiles()).containsExactly("dev");
    }

    @Test
    void shouldAddNoProfileWhenProductionMode() {
        var environment = postProcess("prod", new MockEnvironment()
                .withProperty("arconia.dev.profiles", "custom-dev")
                .withProperty("arconia.test.profiles", "custom-test"));

        assertThat(environment.getActiveProfiles()).isEmpty();
    }

    @Test
    void shouldAddModeProfilesBeforeProgrammaticallySetProfiles() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("custom");

        postProcess("dev", environment);

        assertThat(environment.getActiveProfiles()).containsExactly("dev", "custom");
    }

    @ParameterizedTest
    @ValueSource(strings = { "dev", "test" })
    void shouldNotAddAlreadyActiveProfile(String mode) {
        var environment = postProcess(mode, new MockEnvironment()
                .withProperty("spring.profiles.active", "custom")
                .withProperty(profilesProperty(mode), "custom"));

        assertThat(environment.getActiveProfiles()).containsExactly("custom");
    }

    @Test
    void shouldNotAddAlreadyActiveIndexedProfile() {
        var environment = postProcess("dev", new MockEnvironment()
                .withProperty("spring.profiles.active[0]", "custom")
                .withProperty("arconia.dev.profiles", "custom"));

        // The indexed declaration is only visible through the Binder. Since the profile
        // is recognized as already declared, the post-processor adds nothing on top.
        assertThat(environment.getActiveProfiles()).isEmpty();
    }

    // PROPERTY SOURCES

    @Test
    void shouldPreferCommandLineArgumentsOverOtherSources() {
        var environment = new MockEnvironment().withProperty("arconia.dev.profiles", "other-dev");
        environment.getPropertySources().addFirst(new SimpleCommandLinePropertySource("--arconia.dev.profiles=cli-dev"));

        postProcess("dev", environment);

        assertThat(environment.getActiveProfiles()).containsExactly("cli-dev");
    }

    @Test
    void shouldActivateProfilesFromBootstrapConfigurationFile() {
        var environment = postProcess("dev", new MockEnvironment(), applicationWithBootstrapFile("arconia.dev.profiles=file-dev"));

        assertThat(environment.getActiveProfiles()).containsExactly("file-dev");
        assertThat(environment.getPropertySources().contains(BootstrapEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)).isTrue();
    }

    @Test
    void shouldPreferOtherSourcesOverBootstrapConfigurationFile() {
        var environment = postProcess("dev", new MockEnvironment().withProperty("arconia.dev.profiles", "other-dev"),
                applicationWithBootstrapFile("arconia.dev.profiles=file-dev"));

        assertThat(environment.getActiveProfiles()).containsExactly("other-dev");
    }

    @Test
    void shouldIgnoreUnsupportedPropertiesInBootstrapConfigurationFile(CapturedOutput output) {
        var environment = postProcess("dev", new MockEnvironment(), applicationWithBootstrapFile("""
                arconia.dev.profiles[0]=file-dev
                arconia.bootstrap.mode=prod
                server.port=9090
                """));

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
        var environment = postProcess("prod", new MockEnvironment(), applicationWithBootstrapFile("""
                arconia.dev.profiles=file-dev
                server.port=9090
                """));

        assertThat(environment.getPropertySources().contains(BootstrapEnvironmentPostProcessor.PROPERTY_SOURCE_NAME)).isFalse();
        assertThat(output).doesNotContain("is not supported in the");
    }

    // APPLICATION STARTUP

    @Test
    void shouldLoadProfileSpecificConfigurationDuringApplicationStartup() {
        try (var context = run("dev", "--spring.config.name=bootstrap-profiles")) {
            assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev");
            assertThat(context.getEnvironment().getProperty("greeting")).isEqualTo("dev");
        }
    }

    @Test
    void shouldGivePrecedenceToUserProfilesDuringApplicationStartup() {
        try (var context = run("dev", "--spring.config.name=bootstrap-profiles", "--spring.profiles.active=local")) {
            assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev", "local");
            assertThat(context.getEnvironment().getProperty("greeting")).isEqualTo("local");
            assertThat(context.getEnvironment().getProperty("dev.only")).isEqualTo("true");
        }
    }

    @Test
    void shouldAddModeProfilesBeforeProfilesActivatedInApplicationConfiguration() {
        try (var context = run("dev", "--spring.config.name=profiles-in-config")) {
            assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("dev", "local");
        }
    }

    @Nested
    @SpringBootTest(classes = TestConfig.class)
    @ActiveProfiles("custom")
    class WhenTestActivatesProfiles {

        @Autowired
        Environment environment;

        @Test
        void shouldAddModeProfilesBeforeTestProfiles() {
            assertThat(environment.getActiveProfiles()).containsExactly("test", "custom");
        }

    }

    /**
     * Runs the post-processor with the given bootstrap mode set explicitly.
     */
    private <E extends ConfigurableEnvironment> E postProcess(String mode, E environment) {
        return postProcess(mode, environment, new SpringApplication());
    }

    private <E extends ConfigurableEnvironment> E postProcess(String mode, E environment, SpringApplication application) {
        System.setProperty(BootstrapMode.SYSTEM_PROPERTY, mode);
        try {
            processor.postProcessEnvironment(environment, application);
        } finally {
            System.clearProperty(BootstrapMode.SYSTEM_PROPERTY);
        }
        return environment;
    }

    /**
     * Runs the full startup with the given bootstrap mode set explicitly.
     */
    private static ConfigurableApplicationContext run(String mode, String... args) {
        var application = new SpringApplication(TestConfig.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        System.setProperty(BootstrapMode.SYSTEM_PROPERTY, mode);
        try {
            return application.run(args);
        } finally {
            System.clearProperty(BootstrapMode.SYSTEM_PROPERTY);
        }
    }

    private static String profilesProperty(String mode) {
        return "arconia.%s.profiles".formatted(mode);
    }

    private static SpringApplication applicationWithBootstrapFile(String content) {
        var classLoader = new ClassLoader(BootstrapEnvironmentPostProcessorTests.class.getClassLoader()) {
            @Override
            @Nullable
            public InputStream getResourceAsStream(String name) {
                if (BootstrapConfigurationFile.LOCATION.equals(name)) {
                    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
                }
                return super.getResourceAsStream(name);
            }
        };
        return new SpringApplication(new DefaultResourceLoader(classLoader));
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfig {
    }

}
