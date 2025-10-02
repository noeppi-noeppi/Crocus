package eu.tuxtown.crocus.filter;

import eu.tuxtown.crocus.api.attribute.Attribute;
import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.delegate.DelegateConfiguration;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Predicate;

@NotNullByDefault
public class SimpleFilterConfig {

    private final SimpleFilterType type;

    private final List<SimpleFilter> when;
    private Predicate<String> name;
    private Predicate<String> description;
    private Predicate<String> location;
    private Predicate<URI> url;
    @Nullable private Instant after;
    @Nullable private Instant before;
    private final List<SimpleFilter.DuringPeriod> during;
    private final List<SimpleFilter.DuringPeriod> outside;
    private ZoneId timezone;
    private final Map<Attribute<?>, Predicate<Object>> attributes;
    private Predicate<Event> event;

    public SimpleFilterConfig(SimpleFilterType type) {
        this.type = type;
        this.when = new ArrayList<>();
        this.name = str -> true;
        this.description = str -> true;
        this.location = str -> true;
        this.url = url -> true;
        this.after = null;
        this.before = null;
        this.during = new ArrayList<>();
        this.outside = new ArrayList<>();
        this.timezone = ZoneId.systemDefault();
        this.attributes = new HashMap<>();
        this.event = event -> true;
    }

    public void when(DelegateConfiguration config) {
        this.when.add(config.configure(this.type));
    }

    public void name(Predicate<String> name) {
        this.name = this.name.and(name);
    }

    public void description(Predicate<String> description) {
        this.description = this.description.and(description);
    }

    public void location(Predicate<String> location) {
        this.location = this.location.and(location);
    }

    public void url(Predicate<URI> url) {
        this.url = this.url.and(url);
    }

    public void after(Instant after) {
        this.after = this.after == null || this.after.isBefore(after) ? after : this.after;
    }

    public void before(Instant before) {
        this.before = this.before == null || this.before.isAfter(before) ? before : this.before;
    }

    public void during(LocalDate day) {
        this.during(day, day);
    }

    public void during(LocalDate firstDay, LocalDate lastDay) {
        this.during.add(new SimpleFilter.DuringPeriod(firstDay, lastDay));
    }

    public void outside(LocalDate day) {
        this.outside(day, day);
    }

    public void outside(LocalDate firstDay, LocalDate lastDay) {
        this.outside.add(new SimpleFilter.DuringPeriod(firstDay, lastDay));
    }

    public void timezone(ZoneId timezone) {
        this.timezone = timezone;
    }

    public void attribute(Attribute<?> attribute, Predicate<@Nullable Object> test) {
        this.attributes.compute(attribute, (k, oldFilter) -> oldFilter == null ? test : oldFilter.and(test));
    }

    public void event(Predicate<Event> event) {
        this.event = this.event.and(event);
    }

    public List<SimpleFilter> getWhen() {
        return Collections.unmodifiableList(this.when);
    }

    public Predicate<String> getName() {
        return this.name;
    }

    public Predicate<String> getDescription() {
        return this.description;
    }

    public Predicate<String> getLocation() {
        return this.location;
    }

    public Predicate<URI> getUrl() {
        return this.url;
    }

    @Nullable
    public Instant getAfter() {
        return this.after;
    }

    @Nullable
    public Instant getBefore() {
        return this.before;
    }

    public List<SimpleFilter.DuringPeriod> getDuring() {
        return List.copyOf(this.during);
    }

    public List<SimpleFilter.DuringPeriod> getOutside() {
        return List.copyOf(this.outside);
    }

    public ZoneId getTimezone() {
        return this.timezone;
    }

    public Map<Attribute<?>, Predicate<@Nullable Object>> getAttributes() {
        return Collections.unmodifiableMap(this.attributes);
    }

    public Predicate<Event> getEvent() {
        return this.event;
    }
}
