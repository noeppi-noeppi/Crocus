package eu.tuxtown.crocus.api.service;

import eu.tuxtown.crocus.api.calendar.EventSource;
import eu.tuxtown.crocus.api.delegate.DelegateConfigurable;
import org.jetbrains.annotations.NotNullByDefault;

/**
 * Plugin service class for {@link EventSource event source} types.
 */
@NotNullByDefault
public interface EventSourceType<T extends EventSource, D> extends Nameable, DelegateConfigurable<T, D> {}
