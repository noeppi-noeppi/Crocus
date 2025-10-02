package eu.tuxtown.crocus.sesquiannual;

import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.parameter.Range;
import biweekly.util.Duration;
import biweekly.util.ICalDate;
import eu.tuxtown.crocus.sesquiannual.internal.TimeHelper;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * An {@link VEventInstance event instance} is a specific occurrence of an {@link VEventGroup event group}.
 *
 * @param id An identifier that is chosen to be unique in the {@link ICalendar}, the event originated from.
 * @param query The {@link CalendarQuery calendar query} that was used to create this {@link VEventInstance event instance}.
 * @param event The actual {@link VEvent event} that contains the details for this {@link VEventInstance event instance}.
 * @param startDate The start date of this {@link VEventInstance event instance} encompassing repetition instances and {@link Range#THIS_AND_FUTURE} replacements.
 * @param nominalStartDate The nominal start date of this {@link VEventInstance event instance} is the date that would be the start date
 *                         if only recurrence rules from the main event were in effect and there were no repetition instances.
 */
@NotNullByDefault
public record VEventInstance(String id, CalendarQuery query, VEvent event, ICalDate startDate, ICalDate nominalStartDate) {

    public VEventInstance(String id, CalendarQuery query, VEvent event, ICalDate startDate, ICalDate nominalStartDate) {
        this.id = Objects.requireNonNull(id);
        this.query = Objects.requireNonNull(query);
        this.event = Objects.requireNonNull(event);
        this.startDate = Objects.requireNonNull(startDate);
        this.nominalStartDate = Objects.requireNonNull(nominalStartDate);
    }

    /**
     * Gets the end date of this {@link VEventInstance event instance} if available..
     */
    public Optional<ICalDate> endDate() {
        return this.query.duration(this.event)
                .map(duration -> new Date(this.startDate.getTime() + duration.toMillis()))
                .map(date -> new ICalDate(date, this.startDate.hasTime()));
    }

    /**
     * Gets the duration of this {@link VEventInstance event instance} if available.
     */
    public Optional<Duration> duration() {
        return this.query.duration(this.event);
    }

    /**
     * Gets the {@link VEventTimespan timespan} in which this {@link VEventInstance event instance} takes place.
     */
    public VEventTimespan timespan() {
        return TimeHelper.eventTimespan(this.startDate(), this.endDate(), this.query().timezone(this.event()));
    }
}
