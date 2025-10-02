package eu.tuxtown.crocus.core.sync;

import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.calendar.Calendar;
import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventFilter;
import eu.tuxtown.crocus.api.calendar.EventKey;
import eu.tuxtown.crocus.api.service.CalendarType;
import eu.tuxtown.crocus.api.service.EventSourceType;
import eu.tuxtown.crocus.core.CrocusRuntime;
import eu.tuxtown.crocus.core.configuration.ConfiguredEventSource;
import eu.tuxtown.crocus.core.configuration.ConfiguredService;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@NotNullByDefault
public class EventCollector {

    public static EventCollection collectEvents(Map<String, ConfiguredService<EventSourceType<?, ?>, ConfiguredEventSource>> sources) throws IOException {
        Map<ConfiguredService.Identifier, Map<EventKey, Event>> mapByCalendar = new HashMap<>();
        Map<String, Map<EventKey, Event>> mapBySource = new HashMap<>();
        for (Map.Entry<String, ConfiguredService<EventSourceType<?, ?>, ConfiguredEventSource>> mapEntry : sources.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            Crocus.info("Querying source " + mapEntry.getKey());
            CrocusRuntime.get().increaseLogLayer();
            try {
                ConfiguredService<EventSourceType<?, ?>, ConfiguredEventSource> entry = mapEntry.getValue();

                // Pre-create the by-source entry so sources with 0 events get an entry as well.
                Map<EventKey, Event> currentSourceEventMap = mapBySource.computeIfAbsent(mapEntry.getKey(), k -> new HashMap<>());

                int total = 0;
                int filtered = 0;
                eventLoop:
                for (Event event : entry.value().source().retrieveEvents()) {
                    Event filteredEvent = event;
                    EventKey key = new EventKey(entry.key().moduleName(), entry.id(), event.id());
                    total += 1;
                    for (EventFilter filter : entry.value().filterChain()) {
                        if ((filteredEvent = filter.filter(key, filteredEvent)) == null) continue eventLoop;
                        if (!Objects.equals(filteredEvent.id(), event.id()))
                            throw new IllegalStateException("Filter modified event id. This is not allowed.");
                    }
                    filtered += 1;
                    if (currentSourceEventMap.put(key, filteredEvent) != null) {
                        throw new IllegalStateException("Duplicate event key detected: " + event.id() + " (in source " + mapEntry.getKey() + ")");
                    }
                    for (ConfiguredService<CalendarType<?, ?>, Calendar> recipient : entry.value().recipients()) {
                        if (mapByCalendar.computeIfAbsent(recipient.identifier(), k -> new HashMap<>()).put(key, filteredEvent) != null) {
                            throw new IllegalStateException("Duplicate event key detected: " + event.id() + " (in calendar " + recipient.id() + ")");
                        }
                    }
                }
                Crocus.info("Collected " + filtered + " events." + (entry.value().filterChain().isEmpty() ? "" : " (" + total + " before filtering)"));
            } finally {
                CrocusRuntime.get().decreaseLogLayer();
            }
        }
        return new EventCollection(
                mapByCalendar.entrySet().stream().map(entry -> Map.entry(entry.getKey(), Map.copyOf(entry.getValue()))).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)),
                mapBySource.entrySet().stream().map(entry -> Map.entry(entry.getKey(), entry.getValue().values().stream().sorted(Comparator.comparing(Event::id)).toList())).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    public record EventCollection(Map<ConfiguredService.Identifier, Map<EventKey, Event>> eventsByCalendar, Map<String, List<Event>> eventsBySource) {}
}
