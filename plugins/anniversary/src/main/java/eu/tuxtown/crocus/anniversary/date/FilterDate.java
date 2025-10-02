package eu.tuxtown.crocus.anniversary.date;

import org.jetbrains.annotations.NotNullByDefault;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;

@NotNullByDefault
public class FilterDate implements AnniversaryDate {

    private final AnniversaryDate date;
    private final Predicate<LocalDate> condition;

    public FilterDate(AnniversaryDate date, Predicate<LocalDate> condition) {
        this.date = date;
        this.condition = condition;
    }

    @Override
    public Stream<LocalDate> resolve(LocalDate from, LocalDate to) {
        return this.date.resolve(from, to).filter(this.condition);
    }

    public static AnniversaryDate parseFilter(AnniversaryDate date, String filter) {
        try {
            return parseFilter(date, Integer.parseInt(filter));
        } catch (NumberFormatException e) {
            //
        }
        if (filter.toLowerCase(Locale.ROOT).startsWith("cw")) {
            try {
                int calendarWeek = Integer.parseInt(filter.substring(2).strip());
                return new FilterDate(date, localDate -> getCalendarWeek(localDate) == calendarWeek);
            } catch (NumberFormatException e) {
                //
            }
        }
        return switch (filter.toLowerCase(Locale.ROOT)) {
            case "easter" -> {
                if (date == AlwaysDate.INSTANCE) yield EasterDate.INSTANCE;
                yield new IntersectionDate(List.of(date, EasterDate.INSTANCE));
            }
            case "mon", "monday" -> parseFilter(date, DayOfWeek.MONDAY);
            case "tue", "tuesday" -> parseFilter(date, DayOfWeek.TUESDAY);
            case "wed", "wednesday" -> parseFilter(date, DayOfWeek.WEDNESDAY);
            case "thu", "thursday" -> parseFilter(date, DayOfWeek.THURSDAY);
            case "fri", "friday" -> parseFilter(date, DayOfWeek.FRIDAY);
            case "sat", "saturday" -> parseFilter(date, DayOfWeek.SATURDAY);
            case "sun", "sunday" -> parseFilter(date, DayOfWeek.SUNDAY);
            case "jan", "january" -> parseFilter(date, Month.JANUARY);
            case "feb", "february" -> parseFilter(date, Month.FEBRUARY);
            case "mar", "march" -> parseFilter(date, Month.MARCH);
            case "apr", "april" -> parseFilter(date, Month.APRIL);
            case "may" -> parseFilter(date, Month.MAY);
            case "jun", "june" -> parseFilter(date, Month.JUNE);
            case "jul", "july" -> parseFilter(date, Month.JULY);
            case "aug", "august" -> parseFilter(date, Month.AUGUST);
            case "sep", "september" -> parseFilter(date, Month.SEPTEMBER);
            case "oct", "october" -> parseFilter(date, Month.OCTOBER);
            case "nov", "november" -> parseFilter(date, Month.NOVEMBER);
            case "dec", "december" -> parseFilter(date, Month.DECEMBER);
            default -> throw new IllegalArgumentException("Invalid anniversary date filter: '" + filter + "'");
        };
    }

    public static AnniversaryDate parseFilter(AnniversaryDate date, int dayOfMonth) {
        if (dayOfMonth >= 1 && dayOfMonth <= 31) {
            return new FilterDate(date, localDate -> localDate.getDayOfMonth() == dayOfMonth);
        } else {
            throw new IllegalArgumentException("Invalid anniversary date filter: '" + dayOfMonth + "'");
        }
    }

    public static AnniversaryDate parseFilter(AnniversaryDate date, DayOfWeek dayOfWeek) {
        return new FilterDate(date, localDate -> localDate.getDayOfWeek() == dayOfWeek);
    }

    public static AnniversaryDate parseFilter(AnniversaryDate date, Month month) {
        return new FilterDate(date, localDate -> localDate.getMonth() == month);
    }

    private static int getCalendarWeek(LocalDate date) {
        LocalDate thursdayInSameWeek;
        if (date.getDayOfWeek().ordinal() > DayOfWeek.THURSDAY.ordinal()) {
            thursdayInSameWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.THURSDAY));
        } else {
            thursdayInSameWeek = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.THURSDAY));
        }
        return thursdayInSameWeek.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
    }
}
