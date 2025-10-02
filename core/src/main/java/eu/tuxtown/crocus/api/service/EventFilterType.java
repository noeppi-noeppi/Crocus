package eu.tuxtown.crocus.api.service;

import eu.tuxtown.crocus.api.calendar.EventFilter;
import eu.tuxtown.crocus.api.delegate.DelegateConfigurable;
import org.jetbrains.annotations.NotNullByDefault;

/**
 * Plugin service class for {@link EventFilter event filter} types.
 */
@NotNullByDefault
public interface EventFilterType<T extends EventFilter, D> extends Nameable, DelegateConfigurable<T, D> {}
