package eu.tuxtown.crocus.api.calendar;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * A filter that can filter out specific events.
 */
@NotNullByDefault
public interface EventFilter {

    /**
     * Gets the name of the event filter.
     */
    String name();

    /**
     * Filters an event. Returns {@code null} to discard the event, the given {code event} to keep
     * it or a new event to replace it. The new event must have the same id as the old event.
     */
    @Nullable Event filter(EventKey key, Event event) throws IOException;
}
