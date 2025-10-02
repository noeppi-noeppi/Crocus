package eu.tuxtown.crocus.core.sync;

import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.calendar.Calendar;
import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventKey;
import eu.tuxtown.crocus.api.service.CalendarType;
import eu.tuxtown.crocus.core.CrocusRuntime;
import eu.tuxtown.crocus.core.Main;
import eu.tuxtown.crocus.core.configuration.ConfiguredService;
import eu.tuxtown.crocus.core.configuration.SystemConfiguration;
import eu.tuxtown.crocus.impl.json.EventJson;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

@NotNullByDefault
public class SyncMain {

    @SuppressWarnings("ConfusingMainMethod")
    public static void main(OptionParser options, Main.Action action, String[] args) throws IOException {
        OptionSpec<Void> specNoIncremental = options.accepts("no-incremental", "Clears all calendars and reinserts all events.");
        OptionSpec<Void> specDump = options.accepts("dump", "Dump events to json before syncing them.");

        if (action instanceof Main.Action.ShowHelp) {
            options.printHelpOn(System.out);
        } else if (action instanceof Main.Action.Run(SystemConfiguration systemConfig)) {
            OptionSet set = options.parse(args);

            Crocus.info("Collecting events");
            EventCollector.EventCollection events;
            CrocusRuntime.get().increaseLogLayer();
            try {
                events = EventCollector.collectEvents(systemConfig.sources());
            } finally {
                CrocusRuntime.get().decreaseLogLayer();
            }

            if (set.has(specDump)) {
                Crocus.info("Dumping collected events");
                CrocusRuntime.get().increaseLogLayer();
                try {
                    Path dumpPath = CrocusRuntime.get().path().resolve("event-dump");
                    if (!Files.isDirectory(dumpPath)) Files.createDirectories(dumpPath);
                    for (Map.Entry<String, List<Event>> entry : events.eventsBySource().entrySet()) {
                        Crocus.info("Dumping " + entry.getKey());
                        CrocusRuntime.get().increaseLogLayer();
                        try (Writer writer = Files.newBufferedWriter(dumpPath.resolve(entry.getKey() + ".json"), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                            EventJson.writeEvents(writer, entry.getValue());
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to dump source " + entry.getKey());
                        } finally {
                            CrocusRuntime.get().decreaseLogLayer();
                        }
                    }
                } finally {
                    CrocusRuntime.get().decreaseLogLayer();
                }
            }

            Crocus.info("Syncing calendars");
            CrocusRuntime.get().increaseLogLayer();
            try {
                Path calendarsPath = CrocusRuntime.get().path().resolve("calendars");
                if (!Files.isDirectory(calendarsPath)) Files.createDirectories(calendarsPath);
                for (Map.Entry<String, ConfiguredService<CalendarType<?, ?>, Calendar>> entry : systemConfig.calendars().entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                    // We also have to sync empty calendars as they may contain old events that need to be deleted.
                    Crocus.info("Syncing " + entry.getKey());
                    CrocusRuntime.get().increaseLogLayer();
                    try {
                        Map<EventKey, Event> collectedEvents = events.eventsByCalendar().getOrDefault(entry.getValue().identifier(), Map.of());
                        CalendarUpdater.updateCalendar(calendarsPath, entry.getValue(), collectedEvents, set.has(specNoIncremental));
                    } finally {
                        CrocusRuntime.get().decreaseLogLayer();
                    }
                }
            } finally {
                CrocusRuntime.get().decreaseLogLayer();
            }

            Crocus.info("Done");
        }
    }
}
