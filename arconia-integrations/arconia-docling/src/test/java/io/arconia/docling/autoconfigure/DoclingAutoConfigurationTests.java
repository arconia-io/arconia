package io.arconia.docling.autoconfigure;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import ai.docling.serve.api.DoclingServeApi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DoclingAutoConfiguration}.
 */
class DoclingAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DoclingAutoConfiguration.class, RestClientAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenRestClientMissing() {
        contextRunner.withClassLoader(new FilteredClassLoader(RestClient.class))
                .run(context -> assertThat(context).doesNotHaveBean(DoclingServeApi.class));
    }

    @Test
    void doclingClientWhenDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DoclingServeApi.class);
            assertThat(context).hasSingleBean(DoclingProperties.class);

            DoclingProperties properties = context.getBean(DoclingProperties.class);
            assertThat(properties.getBaseUrl()).isEqualTo(URI.create("http://localhost:5001"));
            assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
        });
    }

    @Test
    void doclingClientWithCustomProperties() {
        contextRunner.withPropertyValues(
                "arconia.docling.base-url=http://custom-host:8080",
                "arconia.docling.connect-timeout=10s",
                "arconia.docling.read-timeout=60s"
        ).run(context -> {
            assertThat(context).hasSingleBean(DoclingServeApi.class);
            assertThat(context).hasSingleBean(DoclingProperties.class);

            DoclingProperties properties = context.getBean(DoclingProperties.class);
            assertThat(properties.getBaseUrl()).isEqualTo(URI.create("http://custom-host:8080"));
            assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
            assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(60));
        });
    }

    @Test
    void customDoclingClientTakesPrecedence() {
        contextRunner.withUserConfiguration(CustomDoclingServeApiConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(DoclingServeApi.class);
                    assertThat(context.getBean(DoclingServeApi.class))
                            .isSameAs(context.getBean(CustomDoclingServeApiConfiguration.class).customDoclingServeApi());
                });
    }

    @Test
    void propertiesAreEnabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DoclingProperties.class);
            DoclingProperties properties = context.getBean(DoclingProperties.class);
            assertThat(properties).isNotNull();
        });
    }

    @Test
    void doclingConnectionDetailsCreatedByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DoclingServeConnectionDetails.class);
            assertThat(context).hasSingleBean(DoclingAutoConfiguration.PropertiesDoclingServeConnectionDetails.class);

            DoclingServeConnectionDetails connectionDetails = context.getBean(DoclingServeConnectionDetails.class);
            assertThat(connectionDetails.getBaseUrl()).isEqualTo(URI.create("http://localhost:5001"));
        });
    }

    @Test
    void doclingConnectionDetailsWithCustomProperties() {
        contextRunner.withPropertyValues("arconia.docling.base-url=http://custom-docling:9999")
                .run(context -> {
                    assertThat(context).hasSingleBean(DoclingServeConnectionDetails.class);

                    DoclingServeConnectionDetails connectionDetails = context.getBean(DoclingServeConnectionDetails.class);
                    assertThat(connectionDetails.getBaseUrl()).isEqualTo(URI.create("http://custom-docling:9999"));
                });
    }

    @Test
    void customDoclingConnectionDetailsTakesPrecedence() {
        contextRunner.withUserConfiguration(CustomDoclingConnectionDetailsConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(DoclingServeConnectionDetails.class);
                    assertThat(context.getBean(DoclingServeConnectionDetails.class))
                            .isSameAs(context.getBean(CustomDoclingConnectionDetailsConfiguration.class).customConnectionDetails());
                    assertThat(context).doesNotHaveBean(DoclingAutoConfiguration.PropertiesDoclingServeConnectionDetails.class);
                });
    }

    @Test
    void doclingClientUsesConnectionDetails() {
        contextRunner.withUserConfiguration(CustomDoclingConnectionDetailsConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(DoclingServeApi.class);
                    DoclingServeConnectionDetails connectionDetails = context.getBean(DoclingServeConnectionDetails.class);
                    assertThat(connectionDetails.getBaseUrl()).isEqualTo(URI.create("http://custom-connection:7777"));
                });
    }

    @Test
    void whenRestClientAutoConfigurationMissingThenDefaultRestClientUsed() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DoclingAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(DoclingServeApi.class);
                    assertThat(context).doesNotHaveBean(RestClient.Builder.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomDoclingServeApiConfiguration {

        private final DoclingServeApi customDoclingServeApi = mock(DoclingServeApi.class);

        @Bean
        DoclingServeApi customDoclingServeApi() {
            return customDoclingServeApi;
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomDoclingConnectionDetailsConfiguration {

        private final DoclingServeConnectionDetails customConnectionDetails = mock(DoclingServeConnectionDetails.class);

        CustomDoclingConnectionDetailsConfiguration() {
            when(customConnectionDetails.getBaseUrl()).thenReturn(URI.create("http://custom-connection:7777"));
        }

        @Bean
        DoclingServeConnectionDetails customConnectionDetails() {
            return customConnectionDetails;
        }

    }

}
