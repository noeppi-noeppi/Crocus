package eu.tuxtown.crocus.impl.json;

import com.google.gson.*;
import eu.tuxtown.crocus.api.attribute.Attribute;
import eu.tuxtown.crocus.api.calendar.Event;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@NotNullByDefault
public class EventJson {

    private static final Gson GSON;
    static {
        GsonBuilder builder = new GsonBuilder();
        builder.disableHtmlEscaping();
        builder.disableJdkUnsafe();
        builder.setStrictness(Strictness.LENIENT);
        builder.setPrettyPrinting();
        builder.serializeNulls();
        GSON = builder.create();
    }

    public static List<Event> readEvents(Reader reader) throws IOException {
        try {
            JsonArray json = GSON.fromJson(reader, JsonArray.class);
            return eventsFromJson(json);
        } catch (JsonIOException e) {
            if (e.getCause() instanceof IOException ioex) {
                throw ioex;
            } else {
                throw e;
            }
        }
    }

    public static void writeEvents(Writer writer, List<Event> events) throws IOException {
        JsonArray json = eventsToJson(events);
        writer.write(GSON.toJson(json) + "\n");
    }

    public static JsonArray eventsToJson(List<Event> events) {
        JsonArray array = new JsonArray();
        for (Event event : events.stream().sorted(Comparator.comparing(Event::id)).toList()) {
            array.add(eventToJson(event));
        }
        return array;
    }

    public static List<Event> eventsFromJson(JsonArray array) {
        return array.asList().stream().map(JsonElement::getAsJsonObject).map(EventJson::eventFromJson).toList();
    }

    public static JsonObject eventToJson(Event event) {
        JsonObject json = new JsonObject();
        json.addProperty("uid", event.id());
        json.addProperty("name", event.name());
        event.description().ifPresent(value -> json.addProperty("description", value));
        event.location().ifPresent(value -> json.addProperty("location", value));
        event.url().ifPresent(value -> json.addProperty("url", value.toString()));
        json.add("time", TimeJson.timesToJson(event.time()));
        if (!event.attributes().isEmpty()) {
            json.add("attributes", AttributeJson.attributesToJson(event.attributes()));
        }
        return json;
    }

    public static Event eventFromJson(JsonObject json) {
        String id = json.getAsJsonPrimitive("uid").getAsString();
        Event.Builder builder = Event.builder(id);
        builder.name(json.getAsJsonPrimitive("name").getAsString());
        if (json.has("description") && !json.get("description").isJsonNull()) {
            builder.description(json.getAsJsonPrimitive("description").getAsString());
        }
        if (json.has("location") && !json.get("location").isJsonNull()) {
            builder.location(json.getAsJsonPrimitive("location").getAsString());
        }
        if (json.has("url") && !json.get("url").isJsonNull()) {
            builder.url(json.getAsJsonPrimitive("url").getAsString());
        }
        TimeJson.timesFromJson(builder, json.getAsJsonObject("time"));
        if (json.has("attributes") && !json.get("attributes").isJsonNull()) {
            for (Map.Entry<Attribute<?>, ?> entry : AttributeJson.attributesFromJson(json.getAsJsonObject("attributes")).entrySet()) {
                uncheckedAttribute(builder, entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    private static <T> void uncheckedAttribute(Event.Builder builder, Attribute<?> attribute, Object value) {
        //noinspection unchecked
        builder.attribute((Attribute<T>) attribute, (T) value);
    }
}
