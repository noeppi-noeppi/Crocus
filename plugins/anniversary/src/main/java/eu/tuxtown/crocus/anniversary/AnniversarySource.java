package eu.tuxtown.crocus.anniversary;

import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventSource;
import org.jetbrains.annotations.NotNullByDefault;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@NotNullByDefault
public class AnniversarySource implements EventSource {

    private final LocalDate since;
    private final LocalDate until;
    private final List<Anniversary> anniversaries;

    public AnniversarySource(AnniversaryConfig cfg) {
        this.since = cfg.getSince();
        this.until = cfg.getUntil();
        this.anniversaries = List.copyOf(cfg.getAnniversaries());
    }

    @Override
    public String name() {
        return "anniversaries";
    }

    @Override
    public List<Event> retrieveEvents() {
        if (this.until.isBefore(this.since)) return List.of();
        List<Event> events = new ArrayList<>();
        for (Anniversary anniversary : this.anniversaries) {
            List<LocalDate> dates = anniversary.date().resolve(this.since, this.until).distinct().sorted().toList();
            for (LocalDate date : dates) {
                Event event = Event.builder(getEventId(anniversary.name(), date))
                        .name(anniversary.name())
                        .description(anniversary.description().apply(date))
                        .day(date, date)
                        .build();
                events.add(event);
            }
        }
        return Collections.unmodifiableList(events);
    }

    private static String getEventId(String name, LocalDate date) {
        int[] normalizedCodePoints = name.toLowerCase(Locale.ROOT).codePoints()
                .filter(chr -> chr < 128 && (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-' || chr == '.'))
                .toArray();
        String normalizedId = new String(normalizedCodePoints, 0, normalizedCodePoints.length);
        return normalizedId  + "-" + date + "-" + String.format("%08X", name.hashCode());
    }
}
