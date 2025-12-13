package io.arconia.docling.autoconfigure;

import java.net.URI;
import java.net.http.HttpClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import ai.docling.serve.api.DoclingServeApi;

import io.arconia.docling.client.DoclingServeClient;

/**
 * Auto-configuration for the Docling integration.
 */
@AutoConfiguration(after = RestClientAutoConfiguration.class)
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(DoclingProperties.class)
public final class DoclingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DoclingServeConnectionDetails.class)
    PropertiesDoclingServeConnectionDetails doclingServeConnectionDetails(DoclingProperties properties) {
        return new PropertiesDoclingServeConnectionDetails(properties);
    }

    @Bean
    @ConditionalOnMissingBean(DoclingServeApi.class)
    DoclingServeClient doclingServeApi(ObjectProvider<RestClient.Builder> restClientBuilder, DoclingServeConnectionDetails connectionDetails, DoclingProperties properties) {
        RestClient restClient = restClientBuilder.getIfAvailable(RestClient::builder)
                .baseUrl(connectionDetails.getBaseUrl())
                .requestFactory(ClientHttpRequestFactoryBuilder.jdk()
                        .withHttpClientCustomizer(builder -> {
                            if (connectionDetails.getBaseUrl().getScheme().equals("http")) {
                                // Docling Serve uses Python FastAPI which causes errors when called from JDK HttpClient.
                                // The HttpClient uses HTTP 2 by default and then falls back to HTTP 1.1 if not supported.
                                // However, the way FastAPI works results in the fallback not happening, making the call fail.
                                builder.version(HttpClient.Version.HTTP_1_1);
                            }
                        })
                        .build(ClientHttpRequestFactorySettings.defaults()
                                .withConnectTimeout(properties.getConnectTimeout())
                                .withReadTimeout(properties.getReadTimeout())))
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

        return factory.createClient(DoclingServeClient.class);
    }

    /**
     * Implementation of {@link DoclingServeConnectionDetails} that uses properties to determine the Docling endpoint.
     */
    static final class PropertiesDoclingServeConnectionDetails implements DoclingServeConnectionDetails {

        private final DoclingProperties properties;

        PropertiesDoclingServeConnectionDetails(DoclingProperties properties) {
            this.properties = properties;
        }

        @Override
        public URI getBaseUrl() {
            return properties.getBaseUrl();
        }

    }

}
