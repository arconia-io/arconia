package io.arconia.dev.services.core.registration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.core.env.Environment;
import org.springframework.core.io.InputStreamSource;

import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestPostgresContainer;

import static io.arconia.dev.services.core.registration.DevServicesTestSupport.resetBootstrapMode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for the ahead-of-time processing of an application registering dev services: their
 * beans are created by instance suppliers that no generated code can stand in for, so they
 * must be left out of the generated code rather than fail the processing.
 */
class DevServicesAotTests {

    @BeforeEach
    @AfterEach
    void reset() {
        resetBootstrapMode();
    }

    @Test
    void processAheadOfTimeLeavesDevServiceBeansOut() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(DevServiceConfiguration.class);
            TestGenerationContext generationContext = new TestGenerationContext();

            assertThatNoException().isThrownBy(() -> new ApplicationContextAotGenerator().processAheadOfTime(context, generationContext));
            generationContext.writeGeneratedContent();

            // The registrar ran, so the beans exist in the context being processed.
            assertThat(context.containsBeanDefinition("devService.container.postgres")).isTrue();
            assertThat(context.containsBeanDefinition("devService.registration.postgres")).isTrue();
            // But none of them is in the generated code.
            Map<String, InputStreamSource> sources = generationContext.getGeneratedFiles().getGeneratedFiles(Kind.SOURCE);
            assertThat(sources).isNotEmpty();
            assertThat(sources.values()).allSatisfy(source ->
                    assertThat(read(source)).doesNotContain("devService.", DevServiceRegistration.class.getSimpleName(), TestPostgresContainer.class.getSimpleName()));
        }
    }

    private static String read(InputStreamSource source) {
        try (var input = source.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(PostgresDevServiceRegistrar.class)
    static class DevServiceConfiguration {}

    static class PostgresDevServiceRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            DevServicesTestSupport.registerPostgresDevService(registry);
        }

    }

}
