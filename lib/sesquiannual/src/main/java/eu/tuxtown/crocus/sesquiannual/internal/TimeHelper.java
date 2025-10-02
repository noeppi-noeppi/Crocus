package eu.tuxtown.crocus.sesquiannual.internal;

import biweekly.component.VEvent;
import biweekly.property.DateTimeStamp;
import biweekly.property.Sequence;
import biweekly.util.Duration;
import biweekly.util.ICalDate;
import eu.tuxtown.crocus.sesquiannual.VEventInstance;
import eu.tuxtown.crocus.sesquiannual.VEventTimespan;
import org.jetbrains.annotations.NotNullByDefault;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;

@NotNullByDefault
public class TimeHelper {

    public static final Comparator<VEvent> SEQUENCE_ORDER;
    public static final Comparator<VEventInstance> CHRONOLOGICAL;
    static {
        SEQUENCE_ORDER = Comparator.comparing((VEvent event) -> Objects.requireNonNullElse(event.getSequence(), new Sequence(0)).getValue())
                .thenComparing((VEvent event) -> Objects.requireNonNullElse(event.getDateTimeStamp(), new DateTimeStamp(new Date(0))).getValue());
        CHRONOLOGICAL = Comparator.comparing((VEventInstance instance) -> toInstant(instance.startDate(), instance.query().timezone(instance.event()), false))
                .thenComparing(instance -> instance.duration().map(Duration::toMillis).orElse((long) 0));
    }

    private static final DateTimeFormatter COMPACT_LOCAL_DATE_FMT = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .toFormatter(Locale.ROOT);

    private static final DateTimeFormatter COMPACT_UTC_TIME_FMT = new DateTimeFormatterBuilder()
            .append(COMPACT_LOCAL_DATE_FMT)
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .toFormatter(Locale.ROOT);

    public static String getTimeId(ICalDate date) {
        if (date.hasTime()) {
            return COMPACT_UTC_TIME_FMT.format(ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC));
        } else {
            // We can't use ICalDate#toInstant in this case as it depends on the system timezone.
            // Construct a LocalDate first.
            return COMPACT_LOCAL_DATE_FMT.format(toLocalDate(date));
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static VEventTimespan eventTimespan(ICalDate start, Optional<ICalDate> end, TimeZone tz) {
        if (start.hasTime() || (end.isPresent() && end.get().hasTime())) {
            Instant startTime = toInstant(start, tz, false);
            Optional<Instant> endTime = end.map(date -> toInstant(date, tz, false))
                    .map(time -> time.isBefore(startTime) ? startTime : time);
            return new VEventTimespan.Time(startTime, endTime);
        } else {
            LocalDate startDate = toLocalDate(start);
            Optional<LocalDate> endDate = end.map(TimeHelper::toLocalDate)
                    .map(date -> date.minusDays(1))
                    .map(date -> date.isBefore(startDate) ? startDate : date);
            return new VEventTimespan.Date(startDate, endDate);
        }
    }

    public static LocalDate toLocalDate(ICalDate date) {
        if (date.hasTime()) throw new IllegalArgumentException("toLocalDate in iCalendar date with time.");
        // biweekly represents ical dates without time as 00:00 on the day in the time zone of the hosts default calendar.
        ZoneId zone = Calendar.getInstance().getTimeZone().toZoneId(); // that should always succeed
        ZonedDateTime zdt = ZonedDateTime.ofInstant(date.toInstant(), zone);
        return zdt.toLocalDate();
    }

    public static Instant toInstant(ICalDate date, TimeZone tz, boolean endOfDay) {
        if (date.hasTime()) {
            return date.toInstant();
        } else {
            LocalDate day = toLocalDate(date);
            if (endOfDay) day = day.plusDays(1);
            // Can't use TimeZone#toZoneId, we must support custom iCal timezones
            long offsetMillis = tz.getOffset(
                    day.get(ChronoField.ERA) >= 1 ? GregorianCalendar.AD : GregorianCalendar.BC,
                    day.get(ChronoField.YEAR_OF_ERA), day.getMonthValue() - 1, day.getDayOfMonth(),
                    day.get(ChronoField.DAY_OF_WEEK) >= 7 ? 1 : (day.get(ChronoField.DAY_OF_WEEK) + 1),
                    0
            );
            return OffsetDateTime.of(day, LocalTime.MIDNIGHT, ZoneOffset.ofTotalSeconds((int) (offsetMillis / 1000))).toInstant();
        }
    }

    public static Comparator<ICalDate> timezoneSorting(TimeZone tz) {
        return Comparator.comparing(date -> toInstant(date, tz, false));
    }
}
