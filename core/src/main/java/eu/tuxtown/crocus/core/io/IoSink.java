package eu.tuxtown.crocus.core.io;

import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.calendar.Calendar;
import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventKey;
import eu.tuxtown.crocus.api.service.CalendarType;
import eu.tuxtown.crocus.core.CrocusRuntime;
import eu.tuxtown.crocus.core.configuration.ConfiguredService;
import eu.tuxtown.crocus.core.configuration.SystemConfiguration;
import eu.tuxtown.crocus.core.sync.CalendarUpdater;
import eu.tuxtown.crocus.impl.json.EventJson;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

@NotNullByDefault
public sealed interface IoSink permits IoSink.File, IoSink.SystemSink {

    default void validate(SystemConfiguration systemConfig) {}
    void push(SystemConfiguration systemConfig, Path calendarsPath, Map<EventKey, Event> events) throws IOException;

    record File(Path path) implements IoSink {

        @Override
        public void push(SystemConfiguration systemConfig, Path calendarsPath, Map<EventKey, Event> events) throws IOException {
            Crocus.info("Writing events to " + this.path().toAbsolutePath().normalize());
            Set<String> usedEventIds = new HashSet<>();
            List<Event> eventList = new ArrayList<>();
            for (Event event : events.values()) {
                if (usedEventIds.add(event.id())) {
                    eventList.add(event);
                } else {
                    throw new IllegalStateException("Can't dump events to file: Duplicate event id in different namespaces: " + event.id());
                }
            }
            try (Writer writer = Files.newBufferedWriter(this.path(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                EventJson.writeEvents(writer, eventList);
            }
        }
    }

    record SystemSink(String sink, boolean incremental) implements IoSink {

        @Override
        public void validate(SystemConfiguration systemConfig) {
            if (!systemConfig.calendars().containsKey(this.sink())) {
                throw new NoSuchElementException("The requested sink '" + this.sink() + "' is not configured in the system configuration.");
            }
        }

        @Override
        public void push(SystemConfiguration systemConfig, Path calendarsPath, Map<EventKey, Event> events) throws IOException {
            if (systemConfig.calendars().containsKey(this.sink())) {
                ConfiguredService<CalendarType<?, ?>, Calendar> service = systemConfig.calendars().get(this.sink());
                Crocus.info("Pushing events to " + this.sink());
                CrocusRuntime.get().increaseLogLayer();
                try {
                    CalendarUpdater.updateCalendar(calendarsPath, service, events, !this.incremental());
                } finally {
                    CrocusRuntime.get().decreaseLogLayer();
                }
            }
        }
    }
}
