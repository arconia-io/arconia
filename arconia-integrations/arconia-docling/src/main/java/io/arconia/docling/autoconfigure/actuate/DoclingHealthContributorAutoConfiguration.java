package io.arconia.docling.autoconfigure.actuate;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthContributorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import io.arconia.core.support.Incubating;
import io.arconia.docling.actuate.DoclingHealthIndicator;
import io.arconia.docling.autoconfigure.client.DoclingClientAutoConfiguration;
import io.arconia.docling.client.DoclingClient;

@AutoConfiguration(after = DoclingClientAutoConfiguration.class)
@ConditionalOnClass({HealthContributor.class, CompositeHealthContributorConfiguration.class})
@ConditionalOnBean(DoclingClient.class)
@ConditionalOnEnabledHealthIndicator("docling")
@Incubating(since = "0.15.0")
public final class DoclingHealthContributorAutoConfiguration extends CompositeHealthContributorConfiguration<DoclingHealthIndicator, DoclingClient> {

    DoclingHealthContributorAutoConfiguration() {
        super(DoclingHealthIndicator::new);
    }

    @Bean
    @ConditionalOnMissingBean(name = { "doclingHealthIndicator", "doclingHealthContributor" })
    HealthContributor doclingHealthContributor(ConfigurableListableBeanFactory beanFactory) {
        return createContributor(beanFactory, DoclingClient.class);
    }

}
