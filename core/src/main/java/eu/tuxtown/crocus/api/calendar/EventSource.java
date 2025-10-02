package eu.tuxtown.crocus.api.calendar;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.util.List;

@NotNullByDefault
public interface EventSource {

    /**
     * Gets the name of the event source.
     */
    String name();

    /**
     * Retrieves the events from the source. This can be called multiple times on the same source.
     */
    List<Event> retrieveEvents() throws IOException;
}
