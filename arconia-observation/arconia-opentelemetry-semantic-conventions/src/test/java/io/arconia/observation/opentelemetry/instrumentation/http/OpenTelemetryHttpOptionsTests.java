package io.arconia.observation.opentelemetry.instrumentation.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Unit tests for {@link OpenTelemetryHttpOptions}.
 */
class OpenTelemetryHttpOptionsTests {

    private static final String SENSITIVE_QUERY_PARAMETERS_PROPERTY =
            "arconia.observations.conventions.opentelemetry.http.sensitive-query-parameters";

    @Test
    void sensitiveQueryParametersDefaultMatchesConfigurationMetadata() throws IOException {
        List<String> metadataDefault = metadataDefaultValue(SENSITIVE_QUERY_PARAMETERS_PROPERTY);

        assertThat(metadataDefault).isEqualTo(new OpenTelemetryHttpOptions().getSensitiveQueryParameters());
    }

    @Test
    void queryParametersToRedactAddsTheAdditionalOnesToTheSensitiveOnes() {
        OpenTelemetryHttpOptions options = new OpenTelemetryHttpOptions();
        options.setSensitiveQueryParameters(List.of("sig"));
        options.setAdditionalSensitiveQueryParameters(List.of("token"));

        assertThat(options.queryParametersToRedact()).containsExactly("sig", "token");
    }

    @Test
    void headerAttributeKeysUseLowerCaseHeaderNames() {
        OpenTelemetryHttpOptions options = new OpenTelemetryHttpOptions();
        options.setCaptureRequestHeaders(List.of("X-Request-Id"));
        options.setCaptureResponseHeaders(List.of("Content-Type"));

        assertThat(options.requestHeaderAttributeKeys()).containsExactly(entry("x-request-id", "http.request.header.x-request-id"));
        assertThat(options.responseHeaderAttributeKeys()).containsExactly(entry("content-type", "http.response.header.content-type"));
    }

    /**
     * Reads the default value of a property from the generated configuration metadata,
     * which the configuration processor can't infer for collections.
     */
    private static List<String> metadataDefaultValue(String propertyName) throws IOException {
        JsonMapper jsonMapper = JsonMapper.shared();
        for (URL resource : Collections.list(OpenTelemetryHttpOptionsTests.class.getClassLoader()
                .getResources("META-INF/spring-configuration-metadata.json"))) {
            try (InputStream inputStream = resource.openStream()) {
                for (JsonNode property : jsonMapper.readTree(inputStream).path("properties")) {
                    if (propertyName.equals(property.path("name").asString())) {
                        List<String> values = new ArrayList<>();
                        property.path("defaultValue").forEach(value -> values.add(value.asString()));
                        return values;
                    }
                }
            }
        }
        throw new AssertionError("Property not found in the configuration metadata: " + propertyName);
    }

}
