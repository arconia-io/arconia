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
import org.springframework.context.annotation.Import;

import io.arconia.core.support.Incubating;
import io.arconia.docling.actuate.DoclingHealthIndicator;
import io.arconia.docling.autoconfigure.actuate.DoclingHealthContributorAutoConfiguration.DoclingHealthContributorConfiguration;
import io.arconia.docling.autoconfigure.client.DoclingClientAutoConfiguration;
import io.arconia.docling.client.DoclingClient;

@AutoConfiguration(after = DoclingClientAutoConfiguration.class)
@ConditionalOnClass({HealthContributor.class, CompositeHealthContributorConfiguration.class, ConditionalOnEnabledHealthIndicator.class})
@ConditionalOnBean(DoclingClient.class)
@Import(DoclingHealthContributorConfiguration.class)
@Incubating(since = "0.15.0")
public final class DoclingHealthContributorAutoConfiguration {

    @ConditionalOnEnabledHealthIndicator("docling")
    static final class DoclingHealthContributorConfiguration extends CompositeHealthContributorConfiguration<DoclingHealthIndicator, DoclingClient> {

        DoclingHealthContributorConfiguration() {
            super(DoclingHealthIndicator::new);
        }

        @Bean
        @ConditionalOnMissingBean(name = { "doclingHealthIndicator", "doclingHealthContributor" })
        HealthContributor doclingHealthContributor(ConfigurableListableBeanFactory beanFactory) {
            return createContributor(beanFactory, DoclingClient.class);
        }

    }

}
