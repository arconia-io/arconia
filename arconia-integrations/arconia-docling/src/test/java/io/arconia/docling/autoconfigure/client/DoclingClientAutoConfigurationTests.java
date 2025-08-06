package io.arconia.docling.autoconfigure.client;

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

import io.arconia.docling.client.DoclingClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DoclingClientAutoConfiguration}.
 */
class DoclingClientAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DoclingClientAutoConfiguration.class, RestClientAutoConfiguration.class));

    @Test
    void autoConfigurationNotActivatedWhenRestClientMissing() {
        contextRunner.withClassLoader(new FilteredClassLoader(RestClient.class))
                .run(context -> assertThat(context).doesNotHaveBean(DoclingClient.class));
    }

    @Test
    void doclingClientWhenDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DoclingClient.class);
            assertThat(context).hasSingleBean(DoclingClientProperties.class);

            DoclingClientProperties properties = context.getBean(DoclingClientProperties.class);
            assertThat(properties.getUrl()).isEqualTo(URI.create("http://localhost:5001"));
            assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
            assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(30));
        });
    }

    @Test
    void doclingClientWithCustomProperties() {
        contextRunner.withPropertyValues(
                "arconia.docling.url=http://custom-host:8080",
                "arconia.docling.connect-timeout=10s",
                "arconia.docling.read-timeout=60s"
        ).run(context -> {
            assertThat(context).hasSingleBean(DoclingClient.class);
            assertThat(context).hasSingleBean(DoclingClientProperties.class);

            DoclingClientProperties properties = context.getBean(DoclingClientProperties.class);
            assertThat(properties.getUrl()).isEqualTo(URI.create("http://custom-host:8080"));
            assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
            assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(60));
        });
    }

    @Test
    void customDoclingClientTakesPrecedence() {
        contextRunner.withUserConfiguration(CustomDoclingClientConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(DoclingClient.class);
                    assertThat(context.getBean(DoclingClient.class))
                            .isSameAs(context.getBean(CustomDoclingClientConfiguration.class).customDoclingClient());
                });
    }

    @Test
    void propertiesAreEnabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DoclingClientProperties.class);
            DoclingClientProperties properties = context.getBean(DoclingClientProperties.class);
            assertThat(properties).isNotNull();
        });
    }

    @Test
    void doclingConnectionDetailsCreatedByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DoclingConnectionDetails.class);
            assertThat(context).hasSingleBean(DoclingClientAutoConfiguration.PropertiesDoclingConnectionDetails.class);

            DoclingConnectionDetails connectionDetails = context.getBean(DoclingConnectionDetails.class);
            assertThat(connectionDetails.getUrl()).isEqualTo(URI.create("http://localhost:5001"));
        });
    }

    @Test
    void doclingConnectionDetailsWithCustomProperties() {
        contextRunner.withPropertyValues("arconia.docling.url=http://custom-docling:9999")
                .run(context -> {
                    assertThat(context).hasSingleBean(DoclingConnectionDetails.class);

                    DoclingConnectionDetails connectionDetails = context.getBean(DoclingConnectionDetails.class);
                    assertThat(connectionDetails.getUrl()).isEqualTo(URI.create("http://custom-docling:9999"));
                });
    }

    @Test
    void customDoclingConnectionDetailsTakesPrecedence() {
        contextRunner.withUserConfiguration(CustomDoclingConnectionDetailsConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(DoclingConnectionDetails.class);
                    assertThat(context.getBean(DoclingConnectionDetails.class))
                            .isSameAs(context.getBean(CustomDoclingConnectionDetailsConfiguration.class).customConnectionDetails());
                    assertThat(context).doesNotHaveBean(DoclingClientAutoConfiguration.PropertiesDoclingConnectionDetails.class);
                });
    }

    @Test
    void doclingClientUsesConnectionDetails() {
        contextRunner.withUserConfiguration(CustomDoclingConnectionDetailsConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(DoclingClient.class);
                    DoclingConnectionDetails connectionDetails = context.getBean(DoclingConnectionDetails.class);
                    assertThat(connectionDetails.getUrl()).isEqualTo(URI.create("http://custom-connection:7777"));
                });
    }

    @Test
    void whenRestClientAutoConfigurationMissingThenDefaultRestClientUsed() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DoclingClientAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasSingleBean(DoclingClient.class);
                    assertThat(context).doesNotHaveBean(RestClient.Builder.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomDoclingClientConfiguration {

        private final DoclingClient customDoclingClient = mock(DoclingClient.class);

        @Bean
        DoclingClient customDoclingClient() {
            return customDoclingClient;
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomDoclingConnectionDetailsConfiguration {

        private final DoclingConnectionDetails customConnectionDetails = mock(DoclingConnectionDetails.class);

        CustomDoclingConnectionDetailsConfiguration() {
            when(customConnectionDetails.getUrl()).thenReturn(URI.create("http://custom-connection:7777"));
        }

        @Bean
        DoclingConnectionDetails customConnectionDetails() {
            return customConnectionDetails;
        }

    }

}
