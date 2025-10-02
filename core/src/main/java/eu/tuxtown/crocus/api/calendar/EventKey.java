package eu.tuxtown.crocus.api.calendar;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.Serializable;

/**
 * A unique identifier for an event.
 *
 * @param pluginId   The plugin, the event originated from.
 * @param sourceName The {@link EventSource source} the event originated from.
 * @param eventId    The {@link Event event} id.
 */
@NotNullByDefault
public record EventKey(String pluginId, String sourceName, String eventId) implements Serializable {}
