package eu.tuxtown.crocus.api.delegate;

import eu.tuxtown.crocus.core.dsl.meta.DelegateConfigurationImpl;
import org.jetbrains.annotations.NotNullByDefault;

/**
 * An interface that can be used as an argument type in which case the method accepts a code block
 * which is captured ad can then be used to configure {@link DelegateConfigurable delegate configurable}
 * objects.
 */
@NotNullByDefault
public sealed interface DelegateConfiguration permits DelegateConfigurationImpl {

    /**
     * Applies this configuration to the provided {@link DelegateConfigurable configurable}.
     */
    <T> T configure(DelegateConfigurable<T, ?> configurable);

    /**
     * Creates a new {@link DelegateConfiguration delegate configuration} that applies this configuration
     * followed by the provided configuration.
     */
    DelegateConfiguration andThen(DelegateConfiguration other);
}
