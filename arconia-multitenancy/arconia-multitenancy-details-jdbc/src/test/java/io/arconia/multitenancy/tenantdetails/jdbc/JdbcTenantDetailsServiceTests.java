package io.arconia.multitenancy.tenantdetails.jdbc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JdbcTenantDetailsService}.
 */
class JdbcTenantDetailsServiceTests {

    @Test
    void whenNullDataSourceThenThrow() {
        assertThatThrownBy(() -> JdbcTenantDetailsService.builder().dataSource(null).build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dataSource cannot be null");
    }

    @Test
    void whenNullJdbcClientThenThrow() {
        assertThatThrownBy(() -> JdbcTenantDetailsService.builder().build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("jdbcClient cannot be null");
    }

}
