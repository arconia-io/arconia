package io.arconia.multitenancy.web.autoconfigure;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;

import io.arconia.multitenancy.web.autoconfigure.HttpTenantResolutionProperties.HttpResolutionMode;
import io.arconia.multitenancy.web.context.resolvers.CookieTenantResolver;
import io.arconia.multitenancy.web.context.resolvers.HeaderTenantResolver;
import io.arconia.multitenancy.web.context.resolvers.OAuth2TenantResolver;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the shipped defaults of the HTTP resolution properties agree with the
 * defaults applied by the components they configure, and with the hand-written
 * configuration metadata.
 * <p>
 * The properties hold literals rather than referencing those defaults, because
 * spring-boot-configuration-processor only reads default values from literal
 * initializers. These tests are what keeps the copies in step.
 */
class HttpTenantResolutionPropertiesDefaultsTests {

    private final HttpTenantResolutionProperties properties = new HttpTenantResolutionProperties();

    @Test
    void headerNameDefaultMatchesResolverDefault() {
        assertThat(properties.getHeader().getHeaderName())
            .isEqualTo(HeaderTenantResolver.builder().build().getTenantHeaderName());
    }

    @Test
    void cookieNameDefaultMatchesResolverDefault() {
        assertThat(properties.getCookie().getCookieName())
            .isEqualTo(CookieTenantResolver.builder().build().getTenantCookieName());
    }

    @Test
    void claimNameDefaultMatchesResolverDefault() {
        assertThat(properties.getOauth2().getClaimName())
            .isEqualTo(OAuth2TenantResolver.builder().build().getTenantClaimName());
    }

    @Test
    void resolutionIsEnabledInHeaderModeByDefault() {
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getResolutionMode()).isEqualTo(HttpResolutionMode.HEADER);
    }

    @Test
    void filterIsEnabledAndRunsLastByDefault() {
        assertThat(properties.getFilter().isEnabled()).isTrue();
        assertThat(properties.getFilter().getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    void noAdditionalIgnorePathsByDefault() {
        assertThat(properties.getFilter().getAdditionalIgnorePaths()).isEmpty();
    }

    @Test
    void conditionalOnPropertyLiteralsMatchTheResolutionModeEnum() {
        // The @ConditionalOnProperty havingValue attributes cannot reference the enum,
        // because annotation values must be constants of type String.
        assertThat(List.of("cookie", "header", "oauth2"))
            .containsExactlyInAnyOrderElementsOf(Arrays.stream(HttpResolutionMode.values())
                .map(mode -> mode.name().toLowerCase(Locale.ROOT))
                .toList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void ignorePathsDefaultMatchesConfigurationMetadata() throws Exception {
        var declaredPaths = (List<String>) metadataDefault("arconia.multitenancy.resolution.http.filter.ignore-paths");

        assertThat(declaredPaths).containsExactlyInAnyOrderElementsOf(properties.getFilter().getIgnorePaths());
    }

    @Test
    void filterOrderDefaultMatchesConfigurationMetadata() throws Exception {
        assertThat(metadataDefault("arconia.multitenancy.resolution.http.filter.order"))
            .isEqualTo(properties.getFilter().getOrder());
    }

    @SuppressWarnings("unchecked")
    private Object metadataDefault(String propertyName) throws Exception {
        try (InputStream input = new ClassPathResource("META-INF/additional-spring-configuration-metadata.json")
            .getInputStream()) {
            Map<String, Object> metadata = JsonMapper.builder().build().readValue(input, Map.class);
            List<Map<String, Object>> declared = (List<Map<String, Object>>) metadata.get("properties");
            return declared.stream()
                .filter(entry -> propertyName.equals(entry.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No metadata entry for " + propertyName))
                .get("defaultValue");
        }
    }

}
