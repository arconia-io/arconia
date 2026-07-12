package io.arconia.dev.services.core.autoconfigure;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.arconia.core.support.Incubating;
import io.arconia.dev.services.api.provider.DevServiceProvider;

/**
 * Validates that no two dev services belonging to the same mutually exclusive category
 * are active at the same time.
 *
 * @see DevServiceProvider
 * @see MultipleDevServicesException
 */
@Incubating
public final class DevServicesConflictValidator {

    /**
     * Validate the given dev service providers, failing with a
     * {@link MultipleDevServicesException} if more than one dev service
     * belongs to the same category.
     */
    public void validate(Collection<DevServiceProvider> providers) {
        providers.stream()
                .collect(Collectors.groupingBy(DevServiceProvider::category))
                .forEach((category, group) -> {
                    if (group.size() > 1) {
                        List<String> names = group.stream().map(DevServiceProvider::name).sorted().toList();
                        throw new MultipleDevServicesException(category, names);
                    }
                });
    }

}
