package eu.tuxtown.crocus.api.calendar;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * A sink where events can be synchronised into.
 */
@NotNullByDefault
public interface Calendar {

    /**
     * Gets the id of this calendar. These must be unique within each plugin. If two calendars share the same id, this
     * is an error. This can be used to detect multiple calendar instances accessing the same downstream calendar.
     */
    String id();

    /**
     * Gets the name of the calendar.
     */
    String name();

    /**
     * Deletes all events from the calendar. This method is used if a full non-incremental update is requested.
     * For example after outside edits brought the calender and Crocus cache out of sync. This method may not
     * leave any events in the calendar. If some events can't be deleted, this method <b>must</b> throw an exception.
     *
     * @return The number of deleted events if known, {@link OptionalInt#empty()} otherwise.
     */
    OptionalInt deleteAllEvents() throws IOException;

    /**
     * Deletes all the given events. If an event from the set doesn't exist, this method must silently ignore that key.
     * This method is used to restore the calendar to a known state when an exception has occurred.
     * Opposed to {@link #updateEvents(CalendarData, CalendarIds)} this works on the calendars internal ids.
     */
    void deleteEvents(Set<String> events) throws IOException;

    /**
     * Updates the events in the calendar.
     */
    void updateEvents(CalendarData data, CalendarIds ids) throws IOException;

    /**
     * The data to sync in a calendar.
     *
     * @param events        The events, the calendar should contain after the sync.
     * @param deletedEvents The event keys that were deleted since the last call.
     */
    record CalendarData(Map<EventKey, Event> events, Set<EventKey> deletedEvents) {}

    /**
     * Provides access to the calendar id map.
     */
    interface CalendarIds {

        /**
         * Gets a known id for the given event or {@code null} if the event is not yet known.
         */
        @Nullable
        String getId(EventKey key);

        /**
         * Gets a calendar internal id for the given event. If the event is already known, an exception will be thrown.
         */
        void setId(EventKey key, String id);
    }
}
