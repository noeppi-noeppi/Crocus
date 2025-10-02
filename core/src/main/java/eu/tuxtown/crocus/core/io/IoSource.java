package eu.tuxtown.crocus.core.io;

import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventKey;
import eu.tuxtown.crocus.core.configuration.ConfiguredService;
import eu.tuxtown.crocus.core.configuration.SystemConfiguration;
import eu.tuxtown.crocus.core.sync.EventCollector;
import eu.tuxtown.crocus.impl.json.EventJson;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@NotNullByDefault
public sealed interface IoSource permits IoSource.File, IoSource.SystemSource, IoSource.SystemCalendar {

    Set<String> sourcesToRun(SystemConfiguration systemConfig);
    Map<EventKey, Event> fetch(SystemConfiguration systemConfig, EventCollector.EventCollection events) throws IOException;

    record File(Path path) implements IoSource {

        @Override
        public Set<String> sourcesToRun(SystemConfiguration systemConfig) {
            return Set.of();
        }

        @Override
        public Map<EventKey, Event> fetch(SystemConfiguration systemConfig, EventCollector.EventCollection events) throws IOException {
            Crocus.info("Reading events from " + this.path().toAbsolutePath().normalize());
            try (Reader reader = Files.newBufferedReader(this.path(), StandardCharsets.UTF_8)) {
                return EventJson.readEvents(reader).stream()
                        .map(event -> Map.entry(new EventKey("core", "file", event.id()), event))
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
            }
        }
    }

    record SystemSource(String source) implements IoSource {

        @Override
        public Set<String> sourcesToRun(SystemConfiguration systemConfig) {
            if (systemConfig.sources().containsKey(this.source())) {
                return Set.of(this.source());
            } else {
                throw new NoSuchElementException("The requested event source '" + this.source() + "' is not configured in the system configuration.");
            }
        }

        @Override
        public Map<EventKey, Event> fetch(SystemConfiguration systemConfig, EventCollector.EventCollection events) {
            if (systemConfig.sources().containsKey(this.source())) {
                ConfiguredService.Identifier identifier = systemConfig.sources().get(this.source()).identifier();
                return events.eventsBySource().getOrDefault(this.source(), List.of()).stream()
                        .map(event -> Map.entry(new EventKey(identifier.moduleName(), identifier.id(), event.id()), event))
                        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
            } else {
                return Map.of();
            }
        }
    }

    record SystemCalendar(String calendar) implements IoSource {

        @Override
        public Set<String> sourcesToRun(SystemConfiguration systemConfig) {
            if (systemConfig.calendars().containsKey(this.calendar())) {
                ConfiguredService.Identifier identifier = systemConfig.calendars().get(this.calendar()).identifier();
                return systemConfig.sources().entrySet().stream()
                        .filter(entry -> entry.getValue().value().recipients().stream()
                                .anyMatch(service -> Objects.equals(identifier, service.identifier()))
                        )
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toUnmodifiableSet());
            } else {
                throw new NoSuchElementException("The requested calendar '" + this.calendar() + "' is not configured in the system configuration.");
            }
        }

        @Override
        public Map<EventKey, Event> fetch(SystemConfiguration systemConfig, EventCollector.EventCollection events) {
            if (systemConfig.calendars().containsKey(this.calendar())) {
                ConfiguredService.Identifier identifier = systemConfig.calendars().get(this.calendar()).identifier();
                return events.eventsByCalendar().getOrDefault(identifier, Map.of());
            } else {
                return Map.of();
            }
        }
    }
}
