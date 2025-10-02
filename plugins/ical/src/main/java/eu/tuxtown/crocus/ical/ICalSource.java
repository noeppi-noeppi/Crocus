package eu.tuxtown.crocus.ical;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.util.ICalDate;
import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventSource;
import eu.tuxtown.crocus.api.resource.Resource;
import eu.tuxtown.crocus.ical.api.ICalAttributes;
import eu.tuxtown.crocus.sesquiannual.*;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAmount;
import java.util.*;

@NotNullByDefault
public class ICalSource implements EventSource {

    private final Resource res;
    private final Charset charset;
    private final ZoneId timezone;
    private final TemporalAmount repeatFor;
    private final TemporalAmount repeatFromNow;
    private final SequenceBehavior sequences;

    public ICalSource(ICalConfig cfg) {
        this.res = cfg.getSource();
        this.charset = cfg.getCharset();
        this.timezone = cfg.getTimezone();
        this.repeatFor = cfg.getRepeatFor();
        this.repeatFromNow = cfg.getRepeatFromNow();
        this.sequences = cfg.getSequences();
    }

    @Override
    public String name() {
        return "ical:" + this.res;
    }

    @Override
    public List<Event> retrieveEvents() throws IOException {
        ICalendar calendar;
        try (Reader reader = this.res.openReader(this.charset)) {
            calendar = Biweekly.parse(reader)
                    .defaultTimezone(TimeZone.getTimeZone(this.timezone))
                    .first();
        }
        if (calendar == null) throw new IOException("Empty iCalendar");
        CalendarQuery query = new CalendarQuery(calendar, this.timezone);

        List<Event> events = new ArrayList<>();
        for (VEventGroup group : query.eventGroups()) {
            ICalDate recurrenceEnd = this.calculateRepeatFor(group);
            List<VEventInstance> instances = group.instances(this.sequences)
                    .takeWhile(instance -> instance.startDate().compareTo(recurrenceEnd) <= 0)
                    .toList();

            for (VEventInstance instance : instances) {
                events.add(this.createEvent(query, instance));
            }
        }
        return Collections.unmodifiableList(events);
    }

    private Event createEvent(CalendarQuery query, VEventInstance instance) {
        VEvent revision = instance.event();
        Event.Builder builder = Event.builder(instance.id());
        builder.name(query.summary(revision));

        query.description(revision).ifPresent(builder::description);
        query.location(revision).ifPresent(builder::location);
        query.url(revision).ifPresent(builder::url);

        switch (instance.timespan()) {
            case VEventTimespan.Date(LocalDate start, Optional<LocalDate> end) -> {
                builder.day(start, end.orElse(start));
            }
            case VEventTimespan.Time(Instant start, Optional<Instant> end) -> {
                if (end.isPresent()) {
                    builder.time(start, end.get());
                } else {
                    builder.time(start);
                }
            }
        }

        query.priority(revision).ifPresent(priority -> builder.attribute(ICalAttributes.PRIORITY, priority));
        query.status(revision).ifPresent(status -> builder.attribute(ICalAttributes.STATUS, status));
        query.color(revision).ifPresent(color -> builder.attribute(ICalAttributes.COLOR, color));
        query.classification(revision).ifPresent(color -> builder.attribute(ICalAttributes.CLASSIFICATION, color));

        return builder.build();
    }

    private ICalDate calculateRepeatFor(VEventGroup group) {
        ZonedDateTime currentEndOfDay = ZonedDateTime.now().with(ChronoField.NANO_OF_DAY, 0).plusDays(1);

        Instant recStart = group.startDate(this.sequences).toInstant();
        ZoneId tz;
        try {
            tz = group.timezone(this.sequences).toZoneId();
        } catch (DateTimeException e) {
            tz = this.timezone;
        }

        // Must use ZDT for calculation involving repeatFor to support years, months, ...
        ZonedDateTime zdtRecStart = ZonedDateTime.ofInstant(recStart, tz);
        Instant recEndSinceEventStart = zdtRecStart.plus(this.repeatFor).toInstant();
        Instant recEndSinceNow = currentEndOfDay.plus(this.repeatFromNow).toInstant();
        Instant recEnd = recEndSinceNow.isAfter(recEndSinceEventStart) ? recEndSinceNow : recEndSinceEventStart;
        return new ICalDate(new Date(recEnd.toEpochMilli()), true);
    }
}
