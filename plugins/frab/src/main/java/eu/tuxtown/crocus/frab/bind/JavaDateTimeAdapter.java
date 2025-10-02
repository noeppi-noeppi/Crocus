package eu.tuxtown.crocus.frab.bind;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaDateTimeAdapter {
    
    private static final Pattern DURATION_PATTERN = Pattern.compile("(?<sign>-?)(?<hours>\\d+):(?<minutes>\\d+)(?::(?<seconds>\\d+)(?:.(?<nanos>\\d+))?)?");

    public static LocalDate parseDate(String string) {
        return LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(string));
    }

    public static String printDate(LocalDate date) {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(date);
    }

    public static LocalTime parseTime(String string) {
        return LocalTime.from(DateTimeFormatter.ISO_LOCAL_TIME.parse(string));
    }

    public static String printTime(LocalTime time) {
        return DateTimeFormatter.ISO_LOCAL_TIME.format(time);
    }

    public static OffsetDateTime parseTimestamp(String string) {
        return OffsetDateTime.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(string));
    }

    public static String printTimestamp(OffsetDateTime timestamp) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(timestamp);
    }
    
    public static ZoneId parseTimeZone(String string) {
        return ZoneId.of(string);
    }
    
    public static String printTimeZone(ZoneId zone) {
        return zone.getId();
    }
    
    public static Duration parseDuration(String string) {
        Matcher m = DURATION_PATTERN.matcher(string);
        if (!m.matches()) throw new DateTimeParseException("Invalid duration", string, 0);
        boolean negative = "-".equals(m.group("sign"));
        long hours = Long.parseLong(Objects.requireNonNullElse(m.group("hours"), "0"));
        long minutes = Long.parseLong(Objects.requireNonNullElse(m.group("minutes"), "0"));
        long seconds = Long.parseLong(Objects.requireNonNullElse(m.group("seconds"), "0"));
        String nanoString = Objects.requireNonNullElse(m.group("nanos"), "0");
        long nanos = Long.parseLong((nanoString + "000000000").substring(0, 9));
        Duration abs = Duration.ofSeconds(3600 * hours + 60 * minutes + seconds, nanos);
        return negative ? abs.negated() : abs;
    }
    
    public static String printDuration(Duration duration) {
        StringBuilder sb = new StringBuilder();
        if (duration.isNegative()) {
            sb.append("-");
            duration = duration.abs();
        }
        
        long seconds = duration.toSeconds();
        long nano = duration.getNano();
        sb.append(String.format("%02d:%02d", seconds / 3600, (seconds / 60) % 60));
        if (nano != 0 || seconds % 60 != 0) sb.append(String.format("%02d", seconds % 60));
        if (nano != 0) sb.append(String.format(".%9d", nano));
        return sb.toString();
    }
}
