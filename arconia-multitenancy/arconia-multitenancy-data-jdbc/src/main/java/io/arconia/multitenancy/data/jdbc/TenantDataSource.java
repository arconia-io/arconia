package io.arconia.multitenancy.data.jdbc;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.util.Assert;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.context.TenantContext;
import io.arconia.multitenancy.core.exceptions.TenantNotFoundException;
import io.arconia.multitenancy.core.tenantdetails.DefaultTenantIdentifierValidator;
import io.arconia.multitenancy.core.tenantdetails.TenantIdentifierValidator;

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

    /**
     * Maximum number of data sources the factory may create when no limit is configured.
     */
    public static final int DEFAULT_MAX_TENANT_DATA_SOURCES = 100;

    private static final Logger logger = LoggerFactory.getLogger(TenantDataSource.class);

    @Nullable
    private final Function<String, DataSource> dataSourceFactory;

    private final TenantIdentifierValidator tenantIdentifierValidator;

    private final int maxTenantDataSources;

    /**
     * Data sources created by the factory, and therefore owned by this instance.
     */
    private final Map<String, DataSource> createdDataSources = new ConcurrentHashMap<>();

    private final AtomicBoolean defaultDataSourceUsageReported = new AtomicBoolean();

    private TenantDataSource(Map<String, DataSource> dataSources,
            @Nullable Function<String, DataSource> dataSourceFactory,
            TenantIdentifierValidator tenantIdentifierValidator, int maxTenantDataSources,
            @Nullable DataSource defaultDataSource) {
        Assert.notNull(dataSources, "dataSources cannot be null");
        Assert.notNull(tenantIdentifierValidator, "tenantIdentifierValidator cannot be null");
        Assert.isTrue(maxTenantDataSources > 0, "maxTenantDataSources must be greater than zero");
        this.dataSourceFactory = dataSourceFactory;
        this.tenantIdentifierValidator = tenantIdentifierValidator;
        this.maxTenantDataSources = maxTenantDataSources;
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

    /**
     * Required by {@link AbstractRoutingDataSource}, which declares it abstract, but never
     * called here: the only caller is the superclass implementation of
     * {@link #determineTargetDataSource()}, which this class overrides in full so that a
     * data source can be created on demand.
     */
    @Override
    @Nullable
    protected Object determineCurrentLookupKey() {
        return TenantContext.getTenantIdentifier();
    }

    /**
     * Already initialized by the constructor, and the target data sources cannot change
     * afterwards, so there is nothing to do when this is used as a Spring bean.
     */
    @Override
    public void afterPropertiesSet() {
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
            reportDefaultDataSourceUsage();
            return defaultDataSource;
        }
        var dataSource = getResolvedDataSources().get(tenantIdentifier);
        return dataSource != null ? dataSource : createDataSource(tenantIdentifier);
    }

    /**
     * Warns the first time the default data source is used, since a tenant context that
     * was expected to be bound and is not reaches another database without any other
     * signal. A {@code ScopedValue} binding is not inherited by threads started from
     * within its scope, so this most often means work was handed to an executor.
     */
    private void reportDefaultDataSourceUsage() {
        if (defaultDataSourceUsageReported.compareAndSet(false, true)) {
            logger.warn(
                    "No tenant bound to the current context, so the default data source was used. This is expected outside a tenant context, such as in a scheduled task, but it also happens when work is handed to another thread, which does not inherit the tenant context. Further occurrences are logged at debug level.");
        }
        else {
            logger.debug("No tenant bound to the current context, so the default data source was used");
        }
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
        // The identifier is validated here rather than trusted, because the tenant context
        // can be bound programmatically, bypassing whatever validation an entry point such
        // as an HTTP filter applies. It is about to select a database.
        tenantIdentifierValidator.validate(tenantIdentifier);
        if (!createdDataSources.containsKey(tenantIdentifier) && createdDataSources.size() >= maxTenantDataSources) {
            throw new TenantNotFoundException(
                    "No data source found for tenant '%s' and the maximum number of tenant data sources (%d) has been reached"
                        .formatted(tenantIdentifier, maxTenantDataSources));
        }
        var dataSource = createdDataSources.computeIfAbsent(tenantIdentifier, identifier -> {
            logger.info("Creating data source for tenant: {}", identifier);
            return dataSourceFactory.apply(identifier);
        });
        if (dataSource == null) {
            throw new TenantNotFoundException("No data source found for tenant '%s'".formatted(tenantIdentifier));
        }
        return dataSource;
    }

    /**
     * Identifiers of the tenants whose data sources were created by the factory, and are
     * therefore currently held open by this instance.
     */
    public Set<String> getCreatedTenantIdentifiers() {
        return Collections.unmodifiableSet(createdDataSources.keySet());
    }

    /**
     * Closes the data sources created by the configured factory. Data sources supplied by
     * the caller are left untouched, since the caller owns their lifecycle.
     * <p>
     * A data source that fails to close does not prevent the others from being closed.
     * The first failure is rethrown once every data source has been visited, with any
     * further failures attached to it as suppressed exceptions.
     */
    @Override
    public void destroy() throws Exception {
        Exception failure = null;
        try {
            for (var entry : createdDataSources.entrySet()) {
                if (entry.getValue() instanceof AutoCloseable closeable) {
                    try {
                        logger.info("Closing data source for tenant: {}", entry.getKey());
                        closeable.close();
                    }
                    catch (Exception ex) {
                        if (failure == null) {
                            failure = ex;
                        }
                        else {
                            failure.addSuppressed(ex);
                        }
                    }
                }
            }
        }
        finally {
            createdDataSources.clear();
        }
        if (failure != null) {
            throw failure;
        }
    }

    public static final class Builder {

        private final Map<String, DataSource> dataSources = new LinkedHashMap<>();

        @Nullable
        private Function<String, DataSource> dataSourceFactory;

        // Never null. The identifier reaching the factory selects a database, so it is
        // validated whether or not the caller configured a validator.
        private TenantIdentifierValidator tenantIdentifierValidator = DefaultTenantIdentifierValidator.builder()
            .build();

        private int maxTenantDataSources = DEFAULT_MAX_TENANT_DATA_SOURCES;

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
         * Validator applied to a tenant identifier before the factory is asked to create
         * a data source for it.
         */
        public Builder tenantIdentifierValidator(TenantIdentifierValidator tenantIdentifierValidator) {
            this.tenantIdentifierValidator = tenantIdentifierValidator;
            return this;
        }

        /**
         * Maximum number of data sources the factory may create. Each one holds a
         * connection pool, so the limit bounds the resources a stream of unknown tenant
         * identifiers can consume. Data sources registered upfront do not count towards
         * it.
         */
        public Builder maxTenantDataSources(int maxTenantDataSources) {
            this.maxTenantDataSources = maxTenantDataSources;
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
            return new TenantDataSource(dataSources, dataSourceFactory, tenantIdentifierValidator,
                    maxTenantDataSources, defaultDataSource);
        }

    }

}
