package io.arconia.multitenancy.core.tenantdetails;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TenantDetails}.
 */
class TenantDetailsTests {

    @Nested
    class Attribute {

        @Test
        void whenKeyPresentWithMatchingTypeThenReturnValue() {
            var tenant = tenantWith(Map.of("region", "eu", "seats", 42));

            assertThat(tenant.attribute("region", String.class)).isEqualTo("eu");
            assertThat(tenant.attribute("seats", Integer.class)).isEqualTo(42);
        }

        @Test
        void whenKeyPresentWithSupertypeThenReturnValue() {
            var tenant = tenantWith(Map.of("seats", 42));

            assertThat(tenant.attribute("seats", Number.class)).isEqualTo(42);
            assertThat(tenant.attribute("seats", Object.class)).isEqualTo(42);
        }

        @Test
        void whenKeyMissingThenReturnNull() {
            var tenant = tenantWith(Map.of("region", "eu"));

            assertThat(tenant.attribute("tier", String.class)).isNull();
        }

        @Test
        void whenValueIsNullThenReturnNull() {
            var attributes = new HashMap<String, Object>();
            attributes.put("region", null);
            var tenant = tenantWith(attributes);

            assertThat(tenant.attribute("region", String.class)).isNull();
        }

        @Test
        void whenKeyPresentWithWrongTypeThenThrow() {
            var tenant = tenantWith(Map.of("region", "eu"));

            assertThatThrownBy(() -> tenant.attribute("region", Integer.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Attribute 'region' is not of type 'java.lang.Integer'")
                .hasCauseInstanceOf(ClassCastException.class);
        }

        @Test
        void whenAttributesNotOverriddenThenReturnNull() {
            var tenant = tenantWith(null);

            assertThat(tenant.attribute("region", String.class)).isNull();
        }

    }

    private static TenantDetails tenantWith(Map<String, Object> attributes) {
        return new TenantDetails() {

            @Override
            public String identifier() {
                return "acme";
            }

            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public Map<String, Object> attributes() {
                return attributes != null ? attributes : TenantDetails.super.attributes();
            }

        };
    }

}
