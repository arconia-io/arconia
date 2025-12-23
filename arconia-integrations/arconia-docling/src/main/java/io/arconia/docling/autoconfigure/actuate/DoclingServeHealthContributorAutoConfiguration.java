package io.arconia.docling.autoconfigure.actuate;

import ai.docling.serve.api.DoclingServeApi;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.autoconfigure.contributor.CompositeHealthContributorConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.arconia.docling.actuate.DoclingServeHealthIndicator;
import io.arconia.docling.autoconfigure.DoclingAutoConfiguration;
import io.arconia.docling.autoconfigure.actuate.DoclingServeHealthContributorAutoConfiguration.DoclingServeHealthContributorConfiguration;

@AutoConfiguration(after = DoclingAutoConfiguration.class)
@ConditionalOnClass({HealthContributor.class, CompositeHealthContributorConfiguration.class, ConditionalOnEnabledHealthIndicator.class})
@ConditionalOnBean(DoclingServeApi.class)
@Import(DoclingServeHealthContributorConfiguration.class)
public final class DoclingServeHealthContributorAutoConfiguration {

    @ConditionalOnEnabledHealthIndicator("docling")
    static final class DoclingServeHealthContributorConfiguration extends CompositeHealthContributorConfiguration<DoclingServeHealthIndicator, DoclingServeApi> {

        DoclingServeHealthContributorConfiguration() {
            super(DoclingServeHealthIndicator::new);
        }

        @Bean
        @ConditionalOnMissingBean(name = { "doclingServeHealthIndicator", "doclingServeHealthContributor" })
        HealthContributor doclingHealthContributor(ConfigurableListableBeanFactory beanFactory) {
            return createContributor(beanFactory, DoclingServeApi.class);
        }

    }

}
