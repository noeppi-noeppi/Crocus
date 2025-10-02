package eu.tuxtown.crocus.core.configuration;

import eu.tuxtown.crocus.api.calendar.Calendar;
import eu.tuxtown.crocus.api.service.CalendarType;
import eu.tuxtown.crocus.api.service.EventSourceType;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Map;

@NotNullByDefault
public record SystemConfiguration(
        Map<String, ConfiguredService<CalendarType<?, ?>, Calendar>> calendars,
        Map<String, ConfiguredService<EventSourceType<?, ?>, ConfiguredEventSource>> sources) {}
