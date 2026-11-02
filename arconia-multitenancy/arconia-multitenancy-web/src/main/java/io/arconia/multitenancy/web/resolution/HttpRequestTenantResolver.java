package io.arconia.multitenancy.web.resolution;

import jakarta.servlet.http.HttpServletRequest;

import io.arconia.core.annotations.Incubating;
import io.arconia.multitenancy.core.resolution.TenantResolver;

/**
 * Strategy used to resolve the current tenant from an HTTP request,
 * following the {@link TenantResolver} contract.
 */
@Incubating
public interface HttpRequestTenantResolver extends TenantResolver<HttpServletRequest> {}
