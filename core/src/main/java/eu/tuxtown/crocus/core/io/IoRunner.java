package eu.tuxtown.crocus.core.io;

import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventKey;
import eu.tuxtown.crocus.api.service.EventSourceType;
import eu.tuxtown.crocus.core.CrocusRuntime;
import eu.tuxtown.crocus.core.configuration.ConfiguredEventSource;
import eu.tuxtown.crocus.core.configuration.ConfiguredService;
import eu.tuxtown.crocus.core.configuration.SystemConfiguration;
import eu.tuxtown.crocus.core.sync.EventCollector;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@NotNullByDefault
public class IoRunner {

    public static void run(SystemConfiguration systemConfig, List<IoSource> sources, List<IoFilter> filters, List<IoSink> sinks) throws IOException {
        Crocus.info("Running IO action");
        EventCollector.EventCollection eventCollection;
        CrocusRuntime.get().increaseLogLayer();
        try {
            Set<String> sourcesToRun = sources.stream()
                    .flatMap(source -> source.sourcesToRun(systemConfig).stream())
                    .collect(Collectors.toUnmodifiableSet());
            sinks.forEach(sink -> sink.validate(systemConfig));

            Map<String, ConfiguredService<EventSourceType<?, ?>, ConfiguredEventSource>> runningSourceMap = systemConfig.sources().entrySet().stream()
                    .filter(entry -> sourcesToRun.contains(entry.getKey())).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
            eventCollection = EventCollector.collectEvents(runningSourceMap);
        } finally {
            CrocusRuntime.get().decreaseLogLayer();
        }

        Crocus.info("Aggregating events");
        Map<EventKey, Event> events;
        CrocusRuntime.get().increaseLogLayer();
        try {
            Map<EventKey, Event> eventMap = new HashMap<>();
            for (IoSource source : sources) {
                for (Map.Entry<EventKey, Event> entry : source.fetch(systemConfig, eventCollection).entrySet()) {
                    if (eventMap.containsKey(entry.getKey())) {
                        throw new IllegalStateException("I have collected multiple events withe same key: " + entry.getKey());
                    } else {
                        eventMap.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            events = Map.copyOf(eventMap);
            Crocus.info("Aggregated " + events.size() + " events.");
        } finally {
            CrocusRuntime.get().decreaseLogLayer();
        }

        Crocus.info("Applying filters.");
        CrocusRuntime.get().increaseLogLayer();
        try {
            for (int i = 0; i < filters.size(); i++) {
                CrocusRuntime.get().increaseLogLayer();
                try {
                    IoFilter filter = filters.get(i);
                    events = Map.copyOf(filter.filter(events));
                    Crocus.info("After filter " + (i + 1) + " (" + filter.filter().name() + "): " + events.size() + " events.");
                } finally {
                    CrocusRuntime.get().decreaseLogLayer();
                }
            }
        } finally {
            CrocusRuntime.get().decreaseLogLayer();
        }

        Crocus.info("Pushing events into sinks");
        CrocusRuntime.get().increaseLogLayer();
        try {
            Path calendarsPath = CrocusRuntime.get().path().resolve("calendars");
            if (!Files.isDirectory(calendarsPath)) Files.createDirectories(calendarsPath);
            for (IoSink sink : sinks) {
                sink.push(systemConfig, calendarsPath, events);
            }
        } finally {
            CrocusRuntime.get().decreaseLogLayer();
        }

        Crocus.info("Done");
    }
}
