package io.arconia.multitenancy.data.jdbc;

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
 * builds a {@link DataSource} that is aware of the current tenant and selects the appropriate tenant-specific datasource.
 *
 */
public class TenantDataSource extends DelegatingDataSource {

    public TenantDataSource(DataSource primary, Function<String, DataSource> tenantDataSourceSupplier) {
        DataSource db = this.create(primary, tenantDataSourceSupplier);
        this.setTargetDataSource(db);
    }

    protected DataSource create(DataSource primaryDataSource,
                                Function<String, DataSource> tenantDataSourceSupplier) {
        Assert.notNull(primaryDataSource, "the management dataSource must not be null");
        var cached = new ConcurrentHashMap<String, DataSource>();
        var pfb = new ProxyFactoryBean();
        for (var interfaceType : new Class<?>[] {DataSource.class, XADataSource.class})
            pfb.addInterface(interfaceType);
        pfb.addAdvice((MethodInterceptor) (invocation) -> {
            var tenant = TenantContext.getTenantIdentifier();
            var currentDataSource = StringUtils.hasText(tenant)
                    ? cached.computeIfAbsent(tenant, tenantDataSourceSupplier) : primaryDataSource;
            return invocation.getMethod().invoke(currentDataSource, invocation.getArguments());
        });
        return (DataSource) Objects.requireNonNull(pfb.getObject());
    }

}
