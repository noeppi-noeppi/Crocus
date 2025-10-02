package eu.tuxtown.crocus.core.configuration;

import eu.tuxtown.crocus.api.calendar.Calendar;
import eu.tuxtown.crocus.api.calendar.EventFilter;
import eu.tuxtown.crocus.api.calendar.EventSource;
import eu.tuxtown.crocus.api.service.CalendarType;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;

@NotNullByDefault
public record ConfiguredEventSource(
        EventSource source,
        List<EventFilter> filterChain,
        List<ConfiguredService<CalendarType<?, ?>, Calendar>> recipients) {}
