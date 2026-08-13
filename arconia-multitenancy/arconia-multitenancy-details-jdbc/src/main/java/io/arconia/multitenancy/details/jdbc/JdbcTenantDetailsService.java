package io.arconia.multitenancy.details.jdbc;

import java.util.LinkedHashMap;
import java.util.List;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.tenantdetails.Tenant;
import io.arconia.multitenancy.core.tenantdetails.TenantDetails;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;

/**
 * An implementation of {@link TenantDetailsService} that uses a relational database as
 * the source for the tenant details, accessed via JDBC.
 * <p>
 * Tenants are read from a {@code tenant_details} table and their attributes from a
 * {@code tenant_details_attributes} table. Schema scripts for the supported databases are
 * bundled with this module and can be applied at startup.
 */
@Incubating
public final class JdbcTenantDetailsService implements TenantDetailsService {

    private static final String SELECT_TENANTS = """
            select td.identifier, td.enabled, tda.attribute_name, tda.attribute_value
            from tenant_details td
            left join tenant_details_attributes tda on td.id = tda.tenant_id
            """;

    private static final String SELECT_TENANTS_ORDERED = SELECT_TENANTS + "order by td.identifier";

    private static final String SELECT_TENANT_BY_IDENTIFIER = SELECT_TENANTS + "where td.identifier = ?";

    private static final ResultSetExtractor<List<Tenant>> TENANT_EXTRACTOR = rs -> {
        var tenants = new LinkedHashMap<String, Tenant.Builder>();
        while (rs.next()) {
            var identifier = rs.getString("identifier");
            var enabled = rs.getBoolean("enabled");
            var tenant = tenants.computeIfAbsent(identifier,
                    _ -> Tenant.builder().identifier(identifier).enabled(enabled));
            var attributeName = rs.getString("attribute_name");
            var attributeValue = rs.getString("attribute_value");
            if (StringUtils.hasText(attributeName) && StringUtils.hasText(attributeValue)) {
                tenant.addAttribute(attributeName, attributeValue);
            }
        }
        return tenants.values().stream().map(Tenant.Builder::build).toList();
    };

    private final JdbcClient jdbcClient;

    private JdbcTenantDetailsService(JdbcClient jdbcClient) {
        Assert.notNull(jdbcClient, "jdbcClient cannot be null");
        this.jdbcClient = jdbcClient;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Loads every tenant, ordered by identifier, together with all their attributes.
     * <p>
     * The result is not paginated, so the whole tenant table and its attributes are read
     * into memory on each call. That is appropriate for administrative use, such as an
     * actuator endpoint, but not for a per-request code path in a deployment with a large
     * number of tenants. Use {@link #loadTenantByIdentifier(String)} there instead.
     */
    @Override
    public List<? extends TenantDetails> loadAllTenants() {
        return query(SELECT_TENANTS_ORDERED);
    }

    @Override
    public @Nullable TenantDetails loadTenantByIdentifier(String identifier) {
        Assert.hasText(identifier, "identifier cannot be null or empty");
        var tenants = query(SELECT_TENANT_BY_IDENTIFIER, identifier);
        return tenants.isEmpty() ? null : tenants.getFirst();
    }

    private List<Tenant> query(String sql, Object... params) {
        var tenants = this.jdbcClient.sql(sql).params(params).query(TENANT_EXTRACTOR);
        return tenants != null ? tenants : List.of();
    }

    public static final class Builder {

        @Nullable
        private JdbcClient jdbcClient;

        private Builder() {}

        /**
         * The client used to access the database holding the tenant details.
         */
        public Builder jdbcClient(JdbcClient jdbcClient) {
            this.jdbcClient = jdbcClient;
            return this;
        }

        /**
         * The data source for the database holding the tenant details. A dedicated
         * {@link JdbcClient} is created for it.
         */
        public Builder dataSource(DataSource dataSource) {
            Assert.notNull(dataSource, "dataSource cannot be null");
            this.jdbcClient = JdbcClient.create(dataSource);
            return this;
        }

        public JdbcTenantDetailsService build() {
            return new JdbcTenantDetailsService(jdbcClient);
        }

    }

}
