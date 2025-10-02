package eu.tuxtown.crocus.api.calendar;

import eu.tuxtown.crocus.api.attribute.Attribute;
import eu.tuxtown.crocus.api.attribute.Attributes;
import eu.tuxtown.crocus.api.attribute.DefaultedAttribute;
import eu.tuxtown.crocus.impl.attribute.AttributeMap;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.*;
import java.util.*;

/**
 * An event in a calendar.
 */
@NotNullByDefault
public final class Event implements Serializable {

    private final String id;
    private final String name;
    private final @Nullable String description;
    private final @Nullable String location;
    private final @Nullable URI url;
    private final EventTime time;
    private final AttributeMap attributes;

    private Event(String id, String name, @Nullable String description, @Nullable String location, @Nullable URI url, EventTime time, Map<Attribute<?>, ?> attributes) {
        this.id = id;
        this.name = name.replace("\r\n", "\n").strip();
        this.description = description == null ? null : description.replace("\r\n", "\n").strip();
        this.location = location == null ? null : location.replace("\r\n", "\n").strip();
        this.url = (url != null && url.isAbsolute()) ? url : null;
        this.time = time;
        this.attributes = new AttributeMap(attributes);
    }

    /**
     * Gets the id of this event. These must be unique within each {@link EventSource source}. The id is used
     * to update existing events instead of creating new ones every time.
     */
    public String id() {
        return this.id;
    }

    /**
     * Gets the name for this event.
     */
    public String name() {
        return this.name;
    }

    /**
     * Gets the description of this event, if available.
     */
    public Optional<String> description() {
        return Optional.ofNullable(this.description);
    }

    /**
     * Gets the location of this event, if available.
     */
    public Optional<String> location() {
        return Optional.ofNullable(this.location);
    }

    /**
     * Gets an {@link URI#isAbsolute() absolute} {@link URI URI} describing this event, if available.
     */
    public Optional<URI> url() {
        return Optional.ofNullable(this.url);
    }

    /**
     * Gets the {@link EventTime} for this event.
     */
    public EventTime time() {
        return this.time;
    }

    /**
     * Gets a non-standard attribute of this event.
     */
    public <T> Optional<T> attribute(Attribute<T> attribute) {
        if (attribute instanceof DefaultedAttribute<T> defaultedAttribute) {
            return Optional.of(this.attributes.get(attribute).orElse(defaultedAttribute.defaultValue()));
        } else {
            return this.attributes.get(attribute);
        }
    }

    /**
     * Gets a non-standard attribute of this event.
     */
    public <T> T defaultedAttribute(DefaultedAttribute<T> attribute) {
        return Objects.requireNonNull(this.attribute(attribute).orElse(null));
    }

    /**
     * Gets all non-standard attribute explicitly set in this event.
     */
    public Map<Attribute<?>, ?> attributes() {
        return this.attributes.map();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Event event)) return false;
        if (!Objects.equals(this.id, event.id) || !Objects.equals(this.name, event.name)
                || !Objects.equals(this.description, event.description) || !Objects.equals(this.location, event.location)
                || !Objects.equals(this.url, event.url) || !Objects.equals(this.time, event.time)) {
            return false;
        }

        // attributes can contain arrays, check accordingly
        if (!Objects.equals(this.attributes.map().keySet(), event.attributes.map().keySet())) return false;
        for (Attribute<?> attr : this.attributes.map().keySet()) {
            if (!doEqual(this.attributes.get(attr), event.attributes.get(attr))) return false;
        }
        return true;
    }

    private static boolean doEqual(Object a, Object b) {
        return switch (a) {
            case boolean[] aa when b instanceof boolean[] bb -> Arrays.equals(aa, bb);
            case byte[]    aa when b instanceof byte[]    bb -> Arrays.equals(aa, bb);
            case char[]    aa when b instanceof char[]    bb -> Arrays.equals(aa, bb);
            case short[]   aa when b instanceof short[]   bb -> Arrays.equals(aa, bb);
            case int[]     aa when b instanceof int[]     bb -> Arrays.equals(aa, bb);
            case long[]    aa when b instanceof long[]    bb -> Arrays.equals(aa, bb);
            case float[]   aa when b instanceof float[]   bb -> Arrays.equals(aa, bb);
            case double[]  aa when b instanceof double[]  bb -> Arrays.equals(aa, bb);
            case Object[]  aa when b instanceof Object[]  bb -> Arrays.deepEquals(aa, bb);
            default                                          -> Objects.equals(a, b);
        };
    }

    @Override
    public int hashCode() {
        // Don't hash on attributes as there could be arrays
        return Objects.hash(this.id, this.name, this.description, this.location, this.time);
    }

    @Override
    public String toString() {
        return "Event{id=" + this.id + ", name='" + this.name + "'" + ", description='" + this.description + "'"
                + ", location='" + this.location + ", url='" + this.url + "', time=" + this.time + "}";
    }

    /**
     * Gets a new event builder.
     */
    public static Builder builder(UUID id) {
        return new Builder(id.toString());
    }

    /**
     * Gets a new event builder.
     */
    public static Builder builder(String id) {
        return new Builder(id);
    }

    /**
     * Gets a new event builder.
     */
    public static Builder builder(Event event) {
        return new Builder(event);
    }

    @NotNullByDefault
    public static class Builder {

        private final String id;
        private @Nullable String name;
        private @Nullable String description;
        private @Nullable String location;
        private @Nullable URI url;
        private @Nullable EventTime time;
        private final Map<Attribute<?>, Object> attributes;

        private Builder(String id) {
            this.id = Objects.requireNonNull(id);
            this.attributes = new HashMap<>();
        }

        private Builder(Event event) {
            this.id = Objects.requireNonNull(event.id);
            this.name = Objects.requireNonNull(event.name);
            this.description = event.description;
            this.location = event.location;
            this.time = Objects.requireNonNull(event.time);
            this.attributes = new HashMap<>(event.attributes.map());
        }

        /**
         * Sets the event name.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the event description.
         */
        public Builder description(String description) {
            this.description = description.isEmpty() ? null : description;
            return this;
        }

        /**
         * Sets the event location.
         */
        public Builder location(String location) {
            this.location = location.isEmpty() ? null : location;
            return this;
        }

        /**
         * Sets the event url. If the provided string is not a valid {@link URI#isAbsolute() absolute} {@link URI}, the
         * url is unset.
         */
        public Builder url(String url) {
            try {
                return this.url(new URI(url.strip()));
            } catch (URISyntaxException e) {
                this.url = null;
                return this;
            }
        }

        /**
         * Sets the event url. If the provided {@link URI} is not {@link URI#isAbsolute() absolute}, the url is unset.
         */
        public Builder url(URI url) {
            this.url = url.isAbsolute() ? url : null;
            return this;
        }

        /**
         * Sets the event start time and leaves an open end.
         */
        public Builder time(Instant start) {
            return this.times(new EventTime.OpenEnd(start));
        }

        /**
         * Sets the event start and end tims.
         */
        public Builder time(Instant start, Instant end) {
            return this.times(new EventTime.Timed(start, end));
        }

        /**
         * Sets the event start and end days. This makes the event an all-day event.
         * Both start and end day completely included in the event timespan.
         */
        public Builder day(LocalDate start, LocalDate end) {
            return this.times(new EventTime.AllDay(start, end));
        }

        /**
         * Sets the event times.
         */
        public Builder times(EventTime times) {
            this.time = times;
            return this;
        }

        /**
         * Sets a non-standard attribute.
         */
        public <T> Builder attribute(Attribute<T> attribute, T value) {
            if (Attributes.get(attribute.name()).stream().noneMatch(found -> found == attribute)) {
                throw new IllegalArgumentException("Attribute not registered: " + attribute);
            }
            this.attributes.put(attribute, Objects.requireNonNull(value));
            return this;
        }

        /**
         * Builds the event.
         */
        public Event build() {
            Objects.requireNonNull(this.name, "Event has no name.");
            Objects.requireNonNull(this.time, "Event has no time.");
            return new Event(this.id, this.name, this.description, this.location, this.url, this.time, Map.copyOf(this.attributes));
        }
    }

    /**
     * The time for an event.
     */
    @NotNullByDefault
    public sealed interface EventTime extends Serializable permits EventTime.OpenEnd, EventTime.Timed, EventTime.AllDay {

        /**
         * Resolves the start time instant within a given time zone.
         */
        Instant start(ZoneId zone);

        /**
         * Resolves the end time instant within a given time zone. If this event is open-end, this is equal
         * to {@link #start(ZoneId)}.
         */
        Instant end(ZoneId zone);

        /**
         * Resolves the start day within a given time zone.
         */
        LocalDate startDay(ZoneId zone);

        /**
         * Resolves the day within a given time zone. If this event is open-end, this is equal
         * to {@link #startDay(ZoneId)}. If an events ends at 00:00 on a day, the previous day is returned.
         */
        LocalDate endDay(ZoneId zone);

        /**
         * An event with a starting time but no known end time.
         */
        @NotNullByDefault
        record OpenEnd(Instant start) implements EventTime {

            @Override
            public String toString() {
                return "{start=" + this.start() + "}";
            }

            @Override
            public Instant start(ZoneId zone) {
                return this.start();
            }

            @Override
            public Instant end(ZoneId zone) {
                return this.start();
            }

            @Override
            public LocalDate startDay(ZoneId zone) {
                return ZonedDateTime.ofInstant(this.start(), zone).toLocalDate();
            }

            @Override
            public LocalDate endDay(ZoneId zone) {
                return ZonedDateTime.ofInstant(this.start(), zone).toLocalDate();
            }
        }

        /**
         * An event with starting and end time.
         */
        @NotNullByDefault
        record Timed(Instant start, Instant end) implements EventTime {

            public Timed(Instant start, Instant end) {
                this.start = start.isBefore(end) ? start : end;
                this.end = start.isBefore(end) ? end : start;
            }

            @Override
            public String toString() {
                return "{start=" + this.start() + ", end=" + this.end() + "}";
            }

            @Override
            public Instant start(ZoneId zone) {
                return this.start();
            }

            @Override
            public Instant end(ZoneId zone) {
                return this.end();
            }

            @Override
            public LocalDate startDay(ZoneId zone) {
                return ZonedDateTime.ofInstant(this.start(), zone).toLocalDate();
            }

            @Override
            public LocalDate endDay(ZoneId zone) {
                ZonedDateTime zdt = ZonedDateTime.ofInstant(this.end(), zone);
                LocalDate date = zdt.toLocalDate();
                if (zdt.toLocalTime().equals(LocalTime.MIDNIGHT)) {
                    date = date.minusDays(1);
                }
                // Special case: events with no duration that fall on 00:00
                LocalDate startDay = this.startDay(zone);
                return startDay.isAfter(date) ? startDay : date;
            }
        }

        /**
         * An all-day event with starting and end dates (both inclusive).
         */
        @NotNullByDefault
        record AllDay(LocalDate start, LocalDate end) implements EventTime {

            public AllDay(LocalDate start, LocalDate end) {
                this.start = start.isBefore(end) ? start : end;
                this.end = start.isBefore(end) ? end : start;
            }

            @Override
            public String toString() {
                return "{start=" + this.start() + ", end=" + this.end() + "}";
            }

            @Override
            public Instant start(ZoneId zone) {
                return ZonedDateTime.of(this.start(), LocalTime.MIDNIGHT, zone).toInstant();
            }

            @Override
            public Instant end(ZoneId zone) {
                // Add one day to include the full end day in the range from start to end instant.
                return ZonedDateTime.of(this.end().plusDays(1), LocalTime.MIDNIGHT, zone).toInstant();
            }

            @Override
            public LocalDate startDay(ZoneId zone) {
                return this.start();
            }

            @Override
            public LocalDate endDay(ZoneId zone) {
                return this.end();
            }
        }
    }
}
