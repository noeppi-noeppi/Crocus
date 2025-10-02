package eu.tuxtown.crocus.core.io;

import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventFilter;
import eu.tuxtown.crocus.api.calendar.EventKey;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@NotNullByDefault
public record IoFilter(@Nullable String namespace, EventFilter filter) {

    public Map<EventKey, Event> filter(Map<EventKey, Event> events) throws IOException {
        Map<EventKey, Event> newEvents = new HashMap<>();
        for (Map.Entry<EventKey, Event> entry : events.entrySet()) {
            if (this.namespace() == null || Objects.equals(this.namespace(), entry.getKey().sourceName())) {
                @Nullable Event filteredEvent = this.filter().filter(entry.getKey(), entry.getValue());
                if (filteredEvent != null) {
                    if (!Objects.equals(entry.getKey().eventId(), filteredEvent.id())) {
                        throw new IllegalStateException("Event filter modified event id. This is not allowed.");
                    } else {
                        newEvents.put(entry.getKey(), filteredEvent);
                    }
                }
            } else {
                newEvents.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(newEvents);
    }
}
