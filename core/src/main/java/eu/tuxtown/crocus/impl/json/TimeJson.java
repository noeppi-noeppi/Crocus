package eu.tuxtown.crocus.impl.json;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import eu.tuxtown.crocus.api.calendar.Event;
import org.jetbrains.annotations.NotNullByDefault;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

@NotNullByDefault
public class TimeJson {

    public static final DateTimeFormatter INPUT_FORMAT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .parseLenient()
            .parseCaseSensitive()
            .optionalStart()
            .appendLiteral("/")
            .optionalEnd()
            .appendZoneId()
            .parseStrict()
            .toFormatter(Locale.ENGLISH);

    public static JsonObject timesToJson(Event.EventTime time) {
        return switch (time) {
            case Event.EventTime.Timed timed -> {
                JsonObject json = new JsonObject();
                json.add("start", instantToJson(timed.start()));
                json.add("end", instantToJson(timed.end()));
                yield json;
            }
            case Event.EventTime.OpenEnd openEnd -> {
                JsonObject json = new JsonObject();
                json.add("start", instantToJson(openEnd.start()));
                yield json;
            }
            case Event.EventTime.AllDay allDay -> {
                JsonObject json = new JsonObject();
                json.add("startDay", dateToJson(allDay.start()));
                json.add("endDay", dateToJson(allDay.end()));
                yield json;
            }
        };
    }

    public static void timesFromJson(Event.Builder builder, JsonObject json) {
        if (json.has("start") && json.has("end")) {
            Instant start = instantFromJson(json.getAsJsonPrimitive("start"));
            Instant end = instantFromJson(json.getAsJsonPrimitive("end"));
            builder.time(start, end);
        } else if (json.has("start")) {
            Instant start = instantFromJson(json.getAsJsonPrimitive("start"));
            builder.time(start);
        } else if (json.has("startDay") && json.has("endDay")) {
            LocalDate start = dateFromJson(json.getAsJsonPrimitive("startDay"));
            LocalDate end = dateFromJson(json.getAsJsonPrimitive("endDay"));
            builder.day(start, end);
        } else {
            throw new IllegalArgumentException("Invalid event times in json.");
        }
    }

    public static JsonPrimitive instantToJson(Instant instant) {
        return new JsonPrimitive(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.ofInstant(instant, ZoneOffset.UTC)));
    }

    public static Instant instantFromJson(JsonPrimitive json) {
        TemporalAccessor temporal = INPUT_FORMAT.parse(json.getAsString());
        return Instant.ofEpochSecond(
                temporal.getLong(ChronoField.INSTANT_SECONDS),
                temporal.getLong(ChronoField.NANO_OF_SECOND)
        );
    }

    public static JsonPrimitive dateToJson(LocalDate date) {
        return new JsonPrimitive(DateTimeFormatter.ISO_LOCAL_DATE.format(date));
    }

    public static LocalDate dateFromJson(JsonPrimitive json) {
        return LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(json.getAsString()));
    }
}
