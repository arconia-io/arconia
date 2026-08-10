package io.arconia.multitenancy.data.jdbc;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.multitenancy.core.tenantdetails.Tenant;
import io.arconia.multitenancy.core.tenantdetails.TenantDetails;
import io.arconia.multitenancy.core.tenantdetails.TenantDetailsManager;

/**
 * a {@link TenantDetailsManager} backed by a JDBC
 */
public class JdbcTenantDetailsManager
        implements TenantDetailsManager {

    private final JdbcClient db;

    private final TransactionTemplate transactionTemplate;


    private final Function<TenantDetailsContainer, Tenant> tenantDetailsContainerToTenantFunction = tdc -> Tenant //
            .builder() //
            .enabled(tdc.enabled()) //
            .attributes(tdc.attributes()) //
            .identifier(tdc.identifier()) //
            .build();

    private final ResultSetExtractor<Collection<TenantDetailsContainer>> resultSetExtractor = rs -> {
        var map = new HashMap<Long, TenantDetailsContainer>();
        while (rs.next()) {
            var tenantId = rs.getLong("tenant_id");
            var identifier = rs.getString("identifier");
            var enabled = rs.getBoolean("enabled");
            var attributeName = rs.getString("attribute_name");
            var attributeValue = rs.getString("attribute_value");
            var tenantDetails = map.computeIfAbsent(tenantId,
                    id -> new TenantDetailsContainer(id, identifier, enabled, new HashMap<>()));
            if (StringUtils.hasText(attributeName) && StringUtils.hasText(attributeValue))
                tenantDetails.attributes().put(attributeName, attributeValue);
        }
        return map.values();
    };

    public JdbcTenantDetailsManager(DataSource dataSource) {
        this.db = JdbcClient.create(dataSource);

        var transactionManager = new JdbcTransactionManager(dataSource);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public TenantDetails createTenant(String identifier, boolean enable, Map<String, Object> attributes) {
        return this.transactionTemplate.execute(_ -> {
            var generatedKeyHolder = new GeneratedKeyHolder();
            this.db.sql(
                            """
                                     insert into tenant_details (identifier, enabled)
                                     values (?, ?) on conflict (identifier) do update set enabled = excluded.enabled
                                    """
                    ) //
                    .params(identifier, enable) //
                    .update(generatedKeyHolder);
            var keys = generatedKeyHolder.getKeys();
            if (Objects.requireNonNull(keys).get("id") instanceof Number idNumber) {
                var id = idNumber.longValue();
                for (var key : attributes.keySet()) {
                    var value = attributes.get(key);
                    this.db.sql("""

                                    insert into tenant_details_attributes (tenant_id, attribute_name, attribute_value)
                                    values (?, ?, ?)
                                    on conflict (tenant_id, attribute_name) do update set attribute_value = excluded.attribute_value;                    """)
                            .params(id, key, value)
                            .update();
                }
            }
            var result = this.loadTenantByIdentifier(identifier);
            Assert.state(null != result, "the result mustn't be null");
            return result;
        });

    }

    @Override
    public List<? extends TenantDetails> loadAllTenants() {
        var sql = """
                  select * from tenant_details td  full outer join tenant_details_attributes tda on  td.id = tda.tenant_id
                """;
        return this.doLoadTenantDetails(sql);
    }

    @Override
    public @Nullable TenantDetails loadTenantByIdentifier(String identifier) {
        var sql = """
                  select * from tenant_details td full outer join tenant_details_attributes tda on td.id = tda.tenant_id where identifier = ?
                """;
        var all = this.doLoadTenantDetails(sql, identifier);
        return all.isEmpty() ? null : all.getFirst();
    }

    private List<Tenant> doLoadTenantDetails(String sql, Object... params) {
        return this.db //
                .sql(sql) //
                .params(params) //
                .query(this.resultSetExtractor)//
                .stream()//
                .map(this.tenantDetailsContainerToTenantFunction)//
                .toList();
    }

    private record TenantDetailsContainer(Long id, String identifier, boolean enabled, Map<String, Object> attributes) {
    }
}
