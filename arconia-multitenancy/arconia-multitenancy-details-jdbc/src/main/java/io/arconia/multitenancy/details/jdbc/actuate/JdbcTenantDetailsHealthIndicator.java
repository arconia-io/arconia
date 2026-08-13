package io.arconia.multitenancy.details.jdbc.actuate;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.details.jdbc.JdbcTenantDetailsService;

/**
 * Health indicator for the database holding the tenant details.
 * <p>
 * The check is a bounded probe against the tenant details table rather than a full read,
 * so that polling it does not scan every tenant.
 *
 * @see JdbcTenantDetailsService
 */
@Incubating
public final class JdbcTenantDetailsHealthIndicator extends AbstractHealthIndicator {

    private static final String PROBE = "select count(*) from tenant_details";

    private final JdbcClient jdbcClient;

    public JdbcTenantDetailsHealthIndicator(JdbcClient jdbcClient) {
        Assert.notNull(jdbcClient, "jdbcClient cannot be null");
        this.jdbcClient = jdbcClient;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Assert.notNull(builder, "builder cannot be null");

        try {
            Long tenants = jdbcClient.sql(PROBE).query(Long.class).single();
            builder.up().withDetail("tenants", tenants);
        }
        catch (Exception ex) {
            builder.down(ex);
        }
    }

}
