package io.arconia.docling.autoconfigure.client;

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

import io.arconia.core.support.Incubating;
import io.arconia.docling.client.DoclingClient;

/**
 * Auto-configuration for the Docling client.
 */
@AutoConfiguration(after = RestClientAutoConfiguration.class)
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(DoclingClientProperties.class)
@Incubating(since = "0.15.0")
public final class DoclingClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DoclingConnectionDetails.class)
    PropertiesDoclingConnectionDetails doclingConnectionDetails(DoclingClientProperties properties) {
        return new PropertiesDoclingConnectionDetails(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    DoclingClient doclingClient(ObjectProvider<RestClient.Builder> restClientBuilder, DoclingConnectionDetails connectionDetails, DoclingClientProperties properties) {
        RestClient restClient = restClientBuilder.getIfAvailable(RestClient::builder)
                .baseUrl(connectionDetails.getUrl())
                .requestFactory(ClientHttpRequestFactoryBuilder.jdk()
                        .withHttpClientCustomizer(builder -> builder.version(HttpClient.Version.HTTP_1_1))
                        .build(ClientHttpRequestFactorySettings.defaults()
                                .withConnectTimeout(properties.getConnectTimeout())
                                .withReadTimeout(properties.getReadTimeout())))
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

        return factory.createClient(DoclingClient.class);
    }

    /**
     * Implementation of {@link DoclingConnectionDetails} that uses properties to determine the Docling endpoint.
     */
    static final class PropertiesDoclingConnectionDetails implements DoclingConnectionDetails {

        private final DoclingClientProperties properties;

        PropertiesDoclingConnectionDetails(DoclingClientProperties properties) {
            this.properties = properties;
        }

        @Override
        public URI getUrl() {
            return properties.getUrl();
        }

    }

}
