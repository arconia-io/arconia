package io.arconia.autoconfigure.multitenancy.web;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.arconia.web.multitenancy.context.annotations.TenantIdentifierArgumentResolver;

/**
 * Register Arconia-specific Spring Web MVC configuration.
 */
@Configuration(proxyBeanMethods = false)
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new TenantIdentifierArgumentResolver());
    }

}
