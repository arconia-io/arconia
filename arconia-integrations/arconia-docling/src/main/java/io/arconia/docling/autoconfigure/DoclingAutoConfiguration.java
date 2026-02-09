package io.arconia.docling.autoconfigure;

import java.net.URI;

import ai.docling.serve.api.DoclingServeApi;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

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
        return DoclingServeClient.builder()
                .restClientBuilder(restClientBuilder.getIfAvailable(RestClient::builder))
                .baseUrl(connectionDetails.getBaseUrl())
                .apiKey(connectionDetails.getApiKey())
                .connectTimeout(properties.getConnectTimeout())
                .readTimeout(properties.getReadTimeout())
                .build();
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

        @Override
        @Nullable
        public String getApiKey() {
            return properties.getApiKey();
        }

    }

}
