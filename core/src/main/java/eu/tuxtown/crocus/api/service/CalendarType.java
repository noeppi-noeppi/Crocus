package eu.tuxtown.crocus.api.service;

import eu.tuxtown.crocus.api.calendar.Calendar;
import eu.tuxtown.crocus.api.delegate.DelegateConfigurable;
import org.jetbrains.annotations.NotNullByDefault;

/**
 * Plugin service class for {@link Calendar calendar} types.
 */
@NotNullByDefault
public interface CalendarType<T extends Calendar, D> extends Nameable, DelegateConfigurable<T, D> {}
