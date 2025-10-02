package eu.tuxtown.crocus.filter;

import eu.tuxtown.crocus.api.attribute.Attribute;
import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventFilter;
import eu.tuxtown.crocus.api.calendar.EventKey;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@NotNullByDefault
public class SimpleFilter implements EventFilter {

    private final List<SimpleFilter> when;
    private final Predicate<String> name;
    private final Predicate<String> description;
    private final Predicate<String> location;
    private final Predicate<URI> url;
    @Nullable private final Instant after;
    @Nullable private final Instant before;
    private final List<DuringPeriod> during;
    private final List<DuringPeriod> outside;
    private final ZoneId timezone;
    private final Map<Attribute<?>, Predicate<@Nullable Object>> attributes;
    private final Predicate<Event> event;

    public SimpleFilter(SimpleFilterConfig cfg) {
        this.when = List.copyOf(cfg.getWhen());
        this.name = cfg.getName();
        this.description = cfg.getDescription();
        this.location = cfg.getLocation();
        this.url = cfg.getUrl();
        this.after = cfg.getAfter();
        this.before = cfg.getBefore();
        this.during = List.copyOf(cfg.getDuring());
        this.outside = List.copyOf(cfg.getOutside());
        this.timezone = cfg.getTimezone();
        this.attributes = Map.copyOf(cfg.getAttributes());
        this.event = cfg.getEvent();
    }

    @Override
    public String name() {
        return "builtin";
    }

    boolean test(Event event) {
        for (SimpleFilter when : this.when) {
            if (!when.test(event)) return true;
        }
        if (!this.name.test(event.name())) return false;
        if (event.description().filter(d -> !this.description.test(d)).isPresent()) return false;
        if (event.location().filter(l -> !this.location.test(l)).isPresent()) return false;
        if (event.url().filter(l -> !this.url.test(l)).isPresent()) return false;
        if (this.after != null && event.time().start(this.timezone).isBefore(this.after)) return false;
        if (this.before != null && event.time().end(this.timezone).isAfter(this.before)) return false;
        if (!this.during.isEmpty() && this.during.stream().noneMatch(period -> period.covers(event.time(), this.timezone))) return false;
        if (!this.outside.isEmpty() && this.outside.stream().anyMatch(period -> period.intersects(event.time(), this.timezone))) return false;

        for (Map.Entry<Attribute<?>, Predicate<@Nullable Object>> entry : this.attributes.entrySet()) {
            if (!entry.getValue().test(event.attribute(entry.getKey()).orElse(null))) return false;
        }
        return this.event.test(event);
    }

    @Override
    public @Nullable Event filter(EventKey key, Event event) {
        return this.test(event) ? event : null;
    }

    @NotNullByDefault
    public record DuringPeriod(LocalDate firstDay, LocalDate lastDay) {

        public DuringPeriod(LocalDate firstDay, LocalDate lastDay) {
            this.firstDay = firstDay.isBefore(lastDay) ? firstDay : lastDay;
            this.lastDay = firstDay.isBefore(lastDay) ? lastDay : firstDay;
        }

        public boolean contains(LocalDate date) {
            return !this.firstDay().isAfter(date) && !this.lastDay().isBefore(date);
        }

        public boolean covers(Event.EventTime times, ZoneId timezone) {
            return !times.startDay(timezone).isBefore(this.firstDay()) && !times.endDay(timezone).isAfter(this.lastDay());
        }

        public boolean intersects(Event.EventTime times, ZoneId timezone) {
            return !times.endDay(timezone).isBefore(this.firstDay()) && !times.startDay(timezone).isAfter(this.lastDay());
        }
    }
}
