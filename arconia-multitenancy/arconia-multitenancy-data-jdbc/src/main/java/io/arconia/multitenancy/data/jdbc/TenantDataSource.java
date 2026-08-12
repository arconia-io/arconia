package io.arconia.multitenancy.data.jdbc;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.context.TenantContext;
import io.arconia.multitenancy.core.exceptions.TenantNotFoundException;

/**
 * A {@link DataSource} that routes each connection request to the {@link DataSource}
 * belonging to the current tenant.
 *
 * <p>
 * Tenant data sources can be registered upfront, created on demand by a factory, or both.
 * When no tenant is bound to the current context, the default data source is used, if one
 * is configured.
 *
 * <pre>{@code
 * TenantDataSource.builder()
 *     .defaultDataSource(adminDataSource)
 *     .dataSourceFactory(this::createDataSource)
 *     .build();
 * }</pre>
 */
@Incubating
public final class TenantDataSource extends AbstractRoutingDataSource implements DisposableBean {

    @Nullable
    private final Function<String, DataSource> dataSourceFactory;

    /**
     * Data sources created by the factory, and therefore owned by this instance.
     */
    private final Map<String, DataSource> createdDataSources = new ConcurrentHashMap<>();

    private TenantDataSource(Map<String, DataSource> dataSources,
            @Nullable Function<String, DataSource> dataSourceFactory, @Nullable DataSource defaultDataSource) {
        Assert.notNull(dataSources, "dataSources cannot be null");
        this.dataSourceFactory = dataSourceFactory;
        setTargetDataSources(new HashMap<>(dataSources));
        if (defaultDataSource != null) {
            setDefaultTargetDataSource(defaultDataSource);
        }
        // An unknown tenant must never fall back to the default data source, which would
        // silently expose another tenant's data.
        setLenientFallback(false);
        initialize();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    @Nullable
    protected Object determineCurrentLookupKey() {
        return TenantContext.getTenantIdentifier();
    }

    @Override
    protected DataSource determineTargetDataSource() {
        var tenantIdentifier = TenantContext.getTenantIdentifier();
        if (tenantIdentifier == null) {
            var defaultDataSource = getResolvedDefaultDataSource();
            if (defaultDataSource == null) {
                throw new TenantNotFoundException(
                        "No tenant found in the current context and no default data source is configured");
            }
            return defaultDataSource;
        }
        var dataSource = getResolvedDataSources().get(tenantIdentifier);
        return dataSource != null ? dataSource : createDataSource(tenantIdentifier);
    }

    /**
     * Creates and caches the data source for the given tenant. The factory is never
     * invoked more than once per tenant, so it must not resolve connections through this
     * same {@link TenantDataSource}.
     */
    private DataSource createDataSource(String tenantIdentifier) {
        if (dataSourceFactory == null) {
            throw new TenantNotFoundException(
                    "No data source found for tenant '%s' and no data source factory is configured"
                        .formatted(tenantIdentifier));
        }
        var dataSource = createdDataSources.computeIfAbsent(tenantIdentifier, dataSourceFactory);
        if (dataSource == null) {
            throw new TenantNotFoundException("No data source found for tenant '%s'".formatted(tenantIdentifier));
        }
        return dataSource;
    }

    /**
     * Closes the data sources created by the configured factory. Data sources supplied by
     * the caller are left untouched, since the caller owns their lifecycle.
     */
    @Override
    public void destroy() throws Exception {
        for (var dataSource : createdDataSources.values()) {
            if (dataSource instanceof AutoCloseable closeable) {
                closeable.close();
            }
        }
        createdDataSources.clear();
    }

    public static final class Builder {

        private final Map<String, DataSource> dataSources = new LinkedHashMap<>();

        @Nullable
        private Function<String, DataSource> dataSourceFactory;

        @Nullable
        private DataSource defaultDataSource;

        private Builder() {}

        /**
         * Data sources to use for the given tenants, registered upfront.
         */
        public Builder dataSources(Map<String, DataSource> dataSources) {
            Assert.notNull(dataSources, "dataSources cannot be null");
            this.dataSources.putAll(dataSources);
            return this;
        }

        /**
         * Data source to use for the given tenant, registered upfront.
         */
        public Builder dataSource(String tenantIdentifier, DataSource dataSource) {
            Assert.hasText(tenantIdentifier, "tenantIdentifier cannot be null or empty");
            Assert.notNull(dataSource, "dataSource cannot be null");
            this.dataSources.put(tenantIdentifier, dataSource);
            return this;
        }

        /**
         * Factory used to create a data source for a tenant that was not registered
         * upfront. The result is cached, so the factory is invoked at most once per
         * tenant, and the created data sources are closed when this bean is destroyed.
         */
        public Builder dataSourceFactory(Function<String, DataSource> dataSourceFactory) {
            Assert.notNull(dataSourceFactory, "dataSourceFactory cannot be null");
            this.dataSourceFactory = dataSourceFactory;
            return this;
        }

        /**
         * Data source to use when no tenant is bound to the current context. Without it,
         * accessing the database outside a tenant context throws
         * {@link TenantNotFoundException}.
         */
        public Builder defaultDataSource(DataSource defaultDataSource) {
            Assert.notNull(defaultDataSource, "defaultDataSource cannot be null");
            this.defaultDataSource = defaultDataSource;
            return this;
        }

        public TenantDataSource build() {
            return new TenantDataSource(dataSources, dataSourceFactory, defaultDataSource);
        }

    }

}
