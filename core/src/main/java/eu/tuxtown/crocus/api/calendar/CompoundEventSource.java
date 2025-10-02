package eu.tuxtown.crocus.api.calendar;

import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.attribute.Attribute;
import eu.tuxtown.crocus.core.CrocusRuntime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An event source that uses several other sources to gather its events.
 */
@NotNullByDefault
public abstract class CompoundEventSource implements EventSource {

    public abstract List<EventSource> retrieveSources() throws IOException;

    @Override
    public List<Event> retrieveEvents() throws IOException {
        List<Event> allEvents = new ArrayList<>();
        for (EventSource source : this.retrieveSources()) {
            Crocus.info("Querying nested source " + this.name() + "." + source.name());
            CrocusRuntime.get().increaseLogLayer();
            try {
                List<Event> events = source.retrieveEvents();
                allEvents.addAll(events.stream().map(event -> prefixEvent(source.name() + ".", event)).toList());
                Crocus.info("Queried nested source " + this.name() + "." + source.name() + " (" + events.size() + " events)");
            } finally {
                CrocusRuntime.get().decreaseLogLayer();
            }
        }
        return Collections.unmodifiableList(allEvents);
    }

    private static Event prefixEvent(String prefix, Event event) {
        Event.Builder builder = Event.builder(prefix + event.id());
        builder.name(event.name());
        event.description().ifPresent(builder::description);
        event.location().ifPresent(builder::location);
        builder.times(event.time());
        for (Map.Entry<Attribute<?>, ?> entry : event.attributes().entrySet()) {
            @SuppressWarnings("unchecked")
            Attribute<Object> objAttribute = (Attribute<@NotNull Object>) entry.getKey();
            builder.attribute(objAttribute, entry.getValue());
        }
        return builder.build();
    }
}
