package io.arconia.multitenancy.core.tenantdetails;

import java.time.Duration;
import java.util.List;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

import io.arconia.core.annotations.Incubating;

/**
 * A {@link TenantDetailsService} that caches the tenants loaded by identifier from
 * another {@link TenantDetailsService}, using Caffeine.
 * <p>
 * Only tenants that exist are cached, disabled ones included. An identifier that matches
 * no tenant is looked up again every time, since identifiers arrive from untrusted input
 * and caching misses would let arbitrary values evict known tenants, and would delay a
 * newly onboarded tenant. Concurrent lookups of the same identifier load it once, and a
 * failed lookup is not cached. {@link #loadAllTenants()} is never cached.
 * <p>
 * A change to a tenant, such as disabling it, takes effect once its entry expires, so the
 * time to live bounds how long a disabled tenant keeps being accepted.
 */
@Incubating
public final class CachingTenantDetailsService implements TenantDetailsService {

    private static final long DEFAULT_MAX_SIZE = 1000;

    private final TenantDetailsService delegate;

    private final Duration timeToLive;

    private final long maxSize;

    private final Cache<String, TenantDetails> cache;

    private CachingTenantDetailsService(TenantDetailsService delegate, Duration timeToLive, long maxSize) {
        Assert.notNull(delegate, "delegate cannot be null");
        Assert.notNull(timeToLive, "timeToLive cannot be null");
        Assert.isTrue(timeToLive.isPositive(), "timeToLive must be positive");
        Assert.isTrue(maxSize > 0, "maxSize must be greater than zero");
        this.delegate = delegate;
        this.timeToLive = timeToLive;
        this.maxSize = maxSize;
        this.cache = Caffeine.newBuilder().expireAfterWrite(timeToLive).maximumSize(maxSize).build();
    }

    /**
     * Time after which a cached tenant is loaded again.
     */
    public Duration getTimeToLive() {
        return timeToLive;
    }

    /**
     * Maximum number of tenants held in the cache.
     */
    public long getMaxSize() {
        return maxSize;
    }

    @Override
    public List<? extends TenantDetails> loadAllTenants() {
        return delegate.loadAllTenants();
    }

    /**
     * Caffeine declares its value type as nullable and stores nothing when the loader
     * returns {@code null}, so misses are not cached. NullAway only reads that from the
     * generic type in its JSpecify mode, hence the suppression.
     */
    @Override
    @Nullable
    @SuppressWarnings("NullAway")
    public TenantDetails loadTenantByIdentifier(String identifier) {
        Assert.hasText(identifier, "identifier cannot be null or empty");
        return cache.get(identifier, delegate::loadTenantByIdentifier);
    }

    /**
     * Creates a new builder for {@link CachingTenantDetailsService}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        @Nullable
        private TenantDetailsService delegate;

        @Nullable
        private Duration timeToLive;

        private long maxSize = DEFAULT_MAX_SIZE;

        private Builder() {}

        /**
         * The service whose tenants are cached.
         */
        public Builder delegate(TenantDetailsService delegate) {
            this.delegate = delegate;
            return this;
        }

        /**
         * Time after which a cached tenant is loaded again.
         */
        public Builder timeToLive(Duration timeToLive) {
            this.timeToLive = timeToLive;
            return this;
        }

        /**
         * Maximum number of tenants held in the cache.
         */
        public Builder maxSize(long maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        /**
         * Builds the {@link CachingTenantDetailsService} instance.
         */
        public CachingTenantDetailsService build() {
            Assert.notNull(delegate, "delegate cannot be null");
            Assert.notNull(timeToLive, "timeToLive cannot be null");
            return new CachingTenantDetailsService(delegate, timeToLive, maxSize);
        }

    }

}
