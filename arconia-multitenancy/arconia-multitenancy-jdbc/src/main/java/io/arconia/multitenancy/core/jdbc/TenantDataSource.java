package io.arconia.multitenancy.core.jdbc;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.multitenancy.core.context.TenantContext;

/**
 * builds a {@link DataSource} that is aware of the current tenant and
 * selects the appropriate tenant-specific datasource or, if configured,
 * the primary (administrative) datasource.
 */
public class TenantDataSource extends DelegatingDataSource {

    public TenantDataSource(Function <String, DataSource> tenantDataSourceSupplier) {
        var db = this.create(null, tenantDataSourceSupplier);
        this.setTargetDataSource(db);
    }

    public TenantDataSource(DataSource primary, Function<String, DataSource> tenantDataSourceSupplier) {
        Assert.notNull(primary, "the primary dataSource must not be null");
        var db = this.create(primary, tenantDataSourceSupplier);
        this.setTargetDataSource(db);
    }

    private DataSource create(DataSource primaryDataSource,
                                Function<String, DataSource> tenantDataSourceSupplier) {
        var cached = new ConcurrentHashMap<String, DataSource>();
        var pfb = new ProxyFactoryBean();
        for (var interfaceType : new Class<?>[] {DataSource.class, XADataSource.class})
            pfb.addInterface(interfaceType);
        pfb.addAdvice((MethodInterceptor) (invocation) -> {
            var tenant = TenantContext.getTenantIdentifier();
            var currentDataSource = StringUtils.hasText(tenant)
                    ? cached.computeIfAbsent(tenant, tenantDataSourceSupplier) : primaryDataSource;
            Assert.notNull(currentDataSource, "no DataSource found for tenant '" + tenant +
                    "' and no primary specified");
            return invocation.getMethod().invoke(currentDataSource, invocation.getArguments());
        });
        return (DataSource) Objects.requireNonNull(pfb.getObject());
    }

}
