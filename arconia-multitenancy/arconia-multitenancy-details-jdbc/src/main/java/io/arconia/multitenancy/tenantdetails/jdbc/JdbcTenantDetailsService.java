package io.arconia.multitenancy.tenantdetails.jdbc;


import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.multitenancy.core.tenantdetails.Tenant;
import io.arconia.multitenancy.core.tenantdetails.TenantDetails;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsService;

/**
 * a {@link TenantDetailsService} backed by a JDBC.
 */
public class JdbcTenantDetailsService implements TenantDetailsService {

    private final ResultSetExtractor<List<TenantDetails>> resultSetExtractor = rs -> {
        var tenants = new HashMap<String, Tenant.Builder>();
        while (rs.next()) {
            var identifier = rs.getString("identifier");
            var enabled = rs.getBoolean("enabled");
            var attributeName = rs.getString("attribute_name");
            var attributeValue = rs.getString("attribute_value");
            var tenant = tenants.computeIfAbsent(identifier, _ -> Tenant.builder().identifier(identifier).enabled(enabled));
            if (StringUtils.hasText(attributeName) && StringUtils.hasText(attributeValue))
                tenant.addAttribute(attributeName, attributeValue);
        }
        return tenants
                .values()
                .stream()
                .map(t -> (TenantDetails) t.build())
                .toList();
    };

    private final String sql = """
            select
                *
            from
              tenant_details td
            left join tenant_details_attributes tda on
                td.id = tda.tenant_id
            """;

    private final JdbcClient jdbcClient;

    public JdbcTenantDetailsService(DataSource jdbcClient) {
        Assert.notNull(jdbcClient, "db cannot be null");
        this.jdbcClient = JdbcClient.create(jdbcClient);
    }

    @Override
    public List<? extends TenantDetails> loadAllTenants() {
        return this.jdbcClient //
                .sql(this.sql) //
                .query(this.resultSetExtractor);
    }

    @Override
    public @Nullable TenantDetails loadTenantByIdentifier(String identifier) {
        var all = this.jdbcClient //
                .sql(this.sql + " where td.identifier = ?") //
                .params(identifier) //
                .query(this.resultSetExtractor);
        return all.isEmpty() ? null : all.getFirst();
    }
}
