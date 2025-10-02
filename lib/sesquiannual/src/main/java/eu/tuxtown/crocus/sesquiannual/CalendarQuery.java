package eu.tuxtown.crocus.sesquiannual;

import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.io.TimezoneAssignment;
import biweekly.io.TimezoneInfo;
import biweekly.property.*;
import biweekly.util.Duration;
import biweekly.util.Google2445Utils;
import biweekly.util.ICalDate;
import biweekly.util.com.google.ical.compat.javautil.DateIterator;
import eu.tuxtown.crocus.sesquiannual.internal.StreamHelper;
import eu.tuxtown.crocus.sesquiannual.internal.TimeHelper;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility class for queries on an {@link ICalendar}.
 */
@NotNullByDefault
public record CalendarQuery(ICalendar calendar, TimeZone defaultTimeZone) {

    public CalendarQuery(ICalendar calendar, ZoneId defaultTimeZone) {
        this(calendar, TimeZone.getTimeZone(defaultTimeZone));
    }

    /**
     * Gets the {@link Summary summary} of an event.
     *
     * @throws IllegalStateException If the event does not specify a {@link Summary SUMMARY} property.
     */
    public String summary(VEvent event) {
        Summary summary = require(event, event.getSummary(), "SUMMARY");
        return Objects.requireNonNull(summary.getValue()).strip();
    }

    /**
     * Gets the {@link Description description} of an event if available.
     */
    public Optional<String> description(VEvent event) {
        Description description = event.getDescription();
        if (description == null) return Optional.empty();
        return Optional.of(Objects.requireNonNull(description.getValue()).strip());
    }

    /**
     * Gets the {@link Location location} of an event if available.
     */
    public Optional<String> location(VEvent event) {
        Location location = event.getLocation();
        if (location == null) return Optional.empty();
        return Optional.of(Objects.requireNonNull(location.getValue()).strip());
    }

    /**
     * Gets the {@link Url url} of an event if available.
     */
    public Optional<URI> url(VEvent event) {
        Url url = event.getUrl();
        if (url == null) return Optional.empty();
        try {
            return Optional.of(new URI(Objects.requireNonNull(url.getValue()).strip()));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets the {@link Geo geo } of an event as a {@code geo:} URI if available.
     */
    public Optional<URI> geoPos(VEvent event) {
        Geo geo = event.getGeo();
        if (geo == null) return Optional.empty();
        try {
            return Optional.of(new URI("geo:" + Objects.requireNonNull(geo.getLatitude()) + "," + Objects.requireNonNull(geo.getLongitude())));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets the {@link Priority priority} of an event if available.
     */
    public OptionalInt priority(VEvent event) {
        Priority priority = event.getPriority();
        if (priority == null) return OptionalInt.empty();
        int prio = Objects.requireNonNull(priority.getValue());
        if (prio < 0 || prio > 9 ) return OptionalInt.empty();
        return OptionalInt.of(prio);
    }

    /**
     * Gets the {@link Status status} of an event if available.
     */
    public Optional<String> status(VEvent event) {
        Status status = event.getStatus();
        if (status == null) return Optional.empty();
        return Optional.of(Objects.requireNonNull(status.getValue()).strip()).filter(s -> !s.isBlank());
    }

    /**
     * Gets the {@link Color color} of an event if available.
     */
    public Optional<String> color(VEvent event) {
        Color color = event.getColor();
        if (color == null) return Optional.empty();
        return Optional.of(Objects.requireNonNull(color.getValue()).strip()).filter(c -> !c.isBlank());
    }

    /**
     * Gets the {@link Classification classification} of an event if available.
     */
    public Optional<String> classification(VEvent event) {
        Classification classification = event.getClassification();
        if (classification == null) return Optional.empty();
        return Optional.of(Objects.requireNonNull(classification.getValue()).strip()).filter(s -> !s.isBlank());
    }

    /**
     * Gets the {@link DateStart start date} of an event.
     *
     * @throws IllegalStateException If the event does not specify a {@link DateStart DTSTART} property.
     */
    public ICalDate startDate(VEvent event) {
        DateStart start = require(event, event.getDateStart(), "DTSTART");
        return Objects.requireNonNull(start.getValue());
    }

    /**
     * Gets the {@link TimeZone timezone} used to declare an events start date.
     */
    public TimeZone timezone(VEvent event) {
        TimezoneInfo tzInfo = this.calendar.getTimezoneInfo();
        TimezoneAssignment tzEvent = tzInfo.getTimezone(require(event, event.getDateStart(), "DTSTART"));
        if (tzEvent != null) return Objects.requireNonNull(tzEvent.getTimeZone());
        TimezoneAssignment tzCalendar = tzInfo.getDefaultTimezone();
        if (tzCalendar != null) return Objects.requireNonNull(tzCalendar.getTimeZone());
        return this.defaultTimeZone();
    }

    /**
     * Gets the {@link DateEnd end date} of an event. If the event does not specify an end date but does specify a
     * {@link Duration duration}, the end date is computed from the duration and the start date.
     */
    public Optional<ICalDate> endDate(VEvent event) {
        DateEnd end = event.getDateEnd();
        DurationProperty duration = event.getDuration();

        if (end != null) {
            return Optional.of(Objects.requireNonNull(end.getValue()));
        } else if (duration != null) {
            ICalDate startDate = this.startDate(event);
            Duration eventDuration = Objects.requireNonNull(duration.getValue());
            if (eventDuration.isPrior()) return Optional.of(startDate);
            ICalDate endDate = new ICalDate(new Date(startDate.getTime() + eventDuration.toMillis()), startDate.hasTime());
            return Optional.of(endDate);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets the {@link Duration duration} of an event. If the event does not specify a duration but does specify an
     * {@link DateEnd end date}, the duration is computed from the start and end dates.
     */
    public Optional<Duration> duration(VEvent event) {
        DateEnd end = event.getDateEnd();
        DurationProperty duration = event.getDuration();

        if (duration != null) {
            return Optional.of(Objects.requireNonNull(duration.getValue()));
        } else if (end != null) {
            ICalDate startDate = this.startDate(event);
            ICalDate endDate = Objects.requireNonNull(end.getValue());
            if (endDate.getTime() < startDate.getTime()) return Optional.of(Duration.diff(startDate, startDate));
            return Optional.of(Duration.diff(startDate, endDate));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Gets the {@link VEventTimespan timespan} in which the provided {@link VEvent event} takes place.
     */
    public VEventTimespan timespan(VEvent event) {
        return TimeHelper.eventTimespan(this.startDate(event), this.endDate(event), this.timezone(event));
    }

    /**
     * Returns a possibly-infinite stream of all recurrences of the provided event.
     */
    public Stream<ICalDate> recurrences(VEvent event) {
        return this.getRecurrences(event, Optional.empty());
    }

    /**
     * Returns a possibly-infinite stream of all recurrences of the provided event.
     *
     * @param recurrenceStart The point in time from which on the recurrence shall be retrieved.
     */
    public Stream<ICalDate> recurrences(VEvent event, Instant recurrenceStart) {
        return this.getRecurrences(event, Optional.of(recurrenceStart));
    }

    Stream<ICalDate> getRecurrences(VEvent event, Optional<Instant> recurrenceStart) {
        TimeZone tz = this.timezone(event);
        DateIterator itr = Google2445Utils.getDateIterator(event, tz);
        recurrenceStart.ifPresent(instant -> itr.advanceTo(new Date(instant.toEpochMilli())));
        Spliterator<Date> spliterator = Spliterators.spliteratorUnknownSize(itr, Spliterator.ORDERED);
        boolean hasTime = this.startDate(event).hasTime();
        Stream<ICalDate> finalStream = StreamSupport.stream(spliterator, false)
                .map(date -> date instanceof ICalDate iCalDate ? iCalDate : new ICalDate(date, hasTime));
        if (recurrenceStart.isPresent()) {
            return finalStream.filter(date -> !recurrenceStart.get().isAfter(TimeHelper.toInstant(date, tz, false)));
        } else {
            return finalStream;
        }
    }

    /**
     * Groups all events in the calendar by their {@link Uid uid} and returns an {@link VEventGroup event group} per
     * uid. Events with no uid are placed in their own group.
     */
    public List<VEventGroup> eventGroups() {
        List<VEvent> noUidEvents = new ArrayList<>();
        Map<String, List<VEvent>> uidMap = new HashMap<>();
        for (VEvent event : this.calendar().getEvents()) {
            Uid uid = event.getUid();
            if (uid != null) {
                uidMap.computeIfAbsent(Objects.requireNonNull(uid.getValue()), k -> new ArrayList<>()).add(event);
            } else {
                noUidEvents.add(event);
            }
        }
        return Stream.concat(
                noUidEvents.stream().map(event -> new VEventGroup(this, this.summary(event), List.of(event))),
                uidMap.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> new VEventGroup(this, entry.getKey(), entry.getValue()))
        ).toList();
    }

    /**
     * Returns a possibly-infinite stream of all {@link VEventInstance instances} in the calendar.
     */
    public Stream<VEventInstance> instances(SequenceBehavior behavior) {
        return this.getInstances(behavior, Optional.empty());
    }

    /**
     * Returns a possibly-infinite stream of all {@link VEventInstance instances} in the calendar.
     *
     * @param recurrenceStart The point in time from which on the recurrence shall be retrieved.
     */
    public Stream<VEventInstance> instances(SequenceBehavior behavior, Instant recurrenceStart) {
        return this.getInstances(behavior, Optional.of(recurrenceStart));
    }

    Stream<VEventInstance> getInstances(SequenceBehavior behavior, Optional<Instant> recurrenceStart) {
        Objects.requireNonNull(behavior);
        List<Stream<VEventInstance>> streams = this.eventGroups().stream()
                .map(group -> group.getInstances(behavior, recurrenceStart))
                .toList();
        return StreamHelper.mergeOrderedStreams(streams, TimeHelper.CHRONOLOGICAL);
    }

    private static <T extends ICalProperty> T require(VEvent event, @Nullable T value, String name) {
        if (value != null) return value;
        String eventId = Objects.requireNonNullElse(event.getUid(), new Uid("")).getValue();
        if (eventId.isEmpty()) {
            throw new IllegalStateException("Missing required property: " + name);
        } else {
            throw new IllegalStateException("Missing required property on event " + eventId + ": " + name);
        }
    }
}
