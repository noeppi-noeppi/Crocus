package eu.tuxtown.crocus.google.calendar;

import com.google.api.client.util.DateTime;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;

public class GoogleTimeHelper {

    // DateTimeFormatter.OFFSET_DATE_TIME has nanosecond precision and second precision timezone offset, both not supported by Google
    private static final DateTimeFormatter GOOGLE_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendOffset("+HH:MM", "Z")
            .toFormatter(Locale.ROOT);

    public static DateTime toDateTime(OffsetDateTime odt) {
        return new DateTime(GOOGLE_DATE_TIME_FORMATTER.format(odt));
    }

    public static DateTime toDateTime(LocalDate date) {
        return new DateTime(DateTimeFormatter.ISO_LOCAL_DATE.format(date));
    }

    public static @Nullable String toIanaZone(ZoneId zone) {
        if (zone instanceof ZoneOffset offset) {
            // Only full-hour offsets are valid timezone ids.
            if (offset.getTotalSeconds() % 3600 != 0) return null;
            int hours = offset.getTotalSeconds() / 3600;
            if (hours == 0) return "Etc/UTC";
            if (hours < -12 || hours > 14) return null;
            return String.format("Etc/GMT%+d", -hours); // Posix timezone signs
        } else {
            // zone region: getId() is a valid timezone id
            String zoneId = zone.getId();
            // filter some exotic cases
            if (zoneId.indexOf('/') < 0) return null;
            return zoneId;
        }
    }
}
