package eu.tuxtown.crocus.google.calendar;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event.Reminders;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventKey;
import eu.tuxtown.crocus.google.GoogleAuth;
import eu.tuxtown.crocus.google.api.GoogleAttributes;
import eu.tuxtown.crocus.google.request.HandledRequest;
import eu.tuxtown.crocus.google.request.RequestExecutor;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.time.*;
import java.util.*;

@NotNullByDefault
public class GoogleCalendar implements eu.tuxtown.crocus.api.calendar.Calendar {

    private final String calendarId;
    private final Calendar calendar;
    private final RequestExecutor executor;
    private final ZoneId timezone;
    private final boolean isBirthdayCalendar;

    public GoogleCalendar(GoogleCalendarConfig cfg) {
        try {
            GoogleClientSecrets secrets = GoogleClientSecrets.load(GsonFactory.getDefaultInstance(), new StringReader(cfg.getAuth()));
            Credential credential = GoogleAuth.authorize(secrets);
            this.calendar = new Calendar.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), credential)
                    .setApplicationName("Crocus")
                    .build();
            this.executor = RequestExecutor.getDefault(this.calendar);
            this.calendarId = cfg.getCalendarId();
            this.isBirthdayCalendar = cfg.isBirthdays();
            Optional<com.google.api.services.calendar.model.Calendar> entry = this.executor.tryExecute(this.calendar.calendars().get(this.calendarId), RequestExecutor.NOT_FOUND);
            if (entry.isEmpty()) throw new IllegalStateException("Google calendar " + this.calendarId + " not found.");
            ZoneId timezone = ZoneId.systemDefault();
            try {
                timezone = ZoneId.of(entry.get().getTimeZone());
            } catch (Exception e) {
                //
            }
            this.timezone = timezone;
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to create google calendar", e);
        }
    }

    @Override
    public String id() {
        return (this.isBirthdayCalendar ? "b:" : "") + this.calendarId;
    }

    @Override
    public String name() {
        return (this.isBirthdayCalendar ? "google-birthdays:" : "google:") + this.calendarId;
    }

    @Override
    public OptionalInt deleteAllEvents() throws IOException {
        Events events = this.executor.execute(this.calendar.events().list(this.calendarId)
                .setEventTypes(List.of(this.isBirthdayCalendar ? "birthday" : "default"))
                .setSingleEvents(false));

        // Birthday calendars sometimes list the same event multiple times for mysterious reasons, even if
        // singleEvents is turned off. Therefore we first collect all event ids and deduplicate them before
        // deleting.
        Set<String> allEventIds = new HashSet<>();
        while (true) {
            for (com.google.api.services.calendar.model.Event event : events.getItems()) {
                allEventIds.add(event.getId());
            }
            if (events.getNextPageToken() == null) break;
            events = this.executor.execute(this.calendar.events().list(this.calendarId)
                    .setEventTypes(List.of(this.isBirthdayCalendar ? "birthday" : "default"))
                    .setSingleEvents(false)
                    .setPageToken(events.getNextPageToken()));
        }
        try (RequestExecutor.Batcher batcher = this.executor.newBatcher()) {
            for (String eventId : allEventIds) {
                batcher.enqueue(HandledRequest.of(this.calendar.events().delete(this.calendarId, eventId)));
            }
        }
        return OptionalInt.of(allEventIds.size());
    }

    @Override
    public void deleteEvents(Set<String> events) throws IOException {
        try (RequestExecutor.Batcher batcher = this.executor.newBatcher()) {
            for (String eventId : events) {
                batcher.enqueue(HandledRequest.of(this.calendar.events().delete(this.calendarId, eventId))
                        .failure(RequestExecutor.NOT_FOUND, err -> {})
                );
            }
        }
    }

    @Override
    public void updateEvents(CalendarData data, CalendarIds ids) throws IOException {
        try (RequestExecutor.Batcher batcher = this.executor.newBatcher()) {
            for (EventKey key : data.deletedEvents()) {
                String eventId = ids.getId(key);
                if (eventId != null) {
                    batcher.enqueue(HandledRequest.of(this.calendar.events().delete(this.calendarId, eventId)));
                }
            }
            for (Map.Entry<EventKey, Event> entry : data.events().entrySet()) {
                EventKey eventKey = entry.getKey();
                String eventId = ids.getId(eventKey);
                if (eventId == null) {
                    batcher.enqueue(HandledRequest.of(this.calendar.events().insert(this.calendarId, this.buildEvent(entry.getValue())))
                            .success(newEvent -> ids.setId(eventKey, newEvent.getId()))
                    );
                } else {
                    batcher.enqueue(HandledRequest.of(this.calendar.events().update(this.calendarId, eventId, this.buildEvent(entry.getValue()))));
                }
            }
        }
    }

    private com.google.api.services.calendar.model.Event buildEvent(Event event) {
        if (this.isBirthdayCalendar) {
            return buildBirthdayEvent(event, this.timezone);
        } else {
            return buildRegularEvent(event, this.timezone);
        }
    }

    private static com.google.api.services.calendar.model.Event buildRegularEvent(Event event, ZoneId timezone) {
        com.google.api.services.calendar.model.Event googleEvent = new com.google.api.services.calendar.model.Event();
        googleEvent.setSummary(event.name());

        String description = event.description().orElse("");
        if (event.url().isPresent()) {
            String url = event.url().get().toString();
            if (!description.contains(url)) {
                description = description.strip() + "\n\n" + url;
            }
        }
        description = description.strip();
        if (!description.isEmpty()) googleEvent.setDescription(description);

        event.location().ifPresent(googleEvent::setLocation);

        @Nullable ZoneId zoneOverride = event.attribute(GoogleAttributes.TIMEZONE_OVERRIDE).orElse(null);
        ZoneId zone = Objects.requireNonNullElse(zoneOverride, timezone);

        switch (event.time()) {
            case Event.EventTime.OpenEnd time -> {
                googleEvent.setStart(getTime(time.start(), zone, zoneOverride != null));
                googleEvent.setEnd(getTime(time.start(), zone, zoneOverride != null));
                googleEvent.setEndTimeUnspecified(true);
            }
            case Event.EventTime.Timed time -> {
                googleEvent.setStart(getTime(time.start(), zone, zoneOverride != null));
                googleEvent.setEnd(getTime(time.end(), zone, zoneOverride != null));
                googleEvent.setEndTimeUnspecified(false);
            }
            case Event.EventTime.AllDay time -> {
                googleEvent.setStart(getTime(time.start()));
                googleEvent.setEnd(getTime(time.end().plusDays(1)));
                googleEvent.setEndTimeUnspecified(false);
            }
            case null, default -> throw new IncompatibleClassChangeError();
        }

        googleEvent.setTransparency(event.defaultedAttribute(GoogleAttributes.BLOCKS_TIME) ? "opaque" : "transparent");

        Reminders reminders = new Reminders();
        reminders.setUseDefault(false);
        reminders.setOverrides(List.of());
        googleEvent.setReminders(reminders);

        return googleEvent;
    }

    private static com.google.api.services.calendar.model.Event buildBirthdayEvent(Event event, ZoneId timezone) {
        LocalDate birthDate = event.time().startDay(timezone);
        Event modifiedEvent = Event.builder(event).day(birthDate, birthDate).build();

        com.google.api.services.calendar.model.Event googleEvent = buildRegularEvent(modifiedEvent, timezone);

        // Special requirements imposed to birthdays by Google Calendar
        googleEvent.setEventType("birthday");
        googleEvent.setDescription(null);
        googleEvent.setLocation(null);
        googleEvent.setVisibility("private");
        googleEvent.setTransparency("transparent");
        if (birthDate.getMonth() == Month.FEBRUARY && birthDate.getDayOfMonth() == 29) {
            googleEvent.setRecurrence(java.util.List.of("RRULE:FREQ=YEARLY;BYMONTH=2;BYMONTHDAY=-1"));
        } else {
            googleEvent.setRecurrence(java.util.List.of("RRULE:FREQ=YEARLY"));
        }
        return googleEvent;
    }

    private static EventDateTime getTime(Instant time, ZoneId zone, boolean zoneOverride) {
        EventDateTime edt = new EventDateTime();
        edt.setDateTime(GoogleTimeHelper.toDateTime(OffsetDateTime.ofInstant(time, zone)));
        if (zoneOverride) {
            @Nullable String zoneId = GoogleTimeHelper.toIanaZone(zone);
            if (zoneId != null) edt.setTimeZone(zoneId);
        }
        return edt;
    }

    private static EventDateTime getTime(LocalDate date) {
        EventDateTime edt = new EventDateTime();
        edt.setDate(GoogleTimeHelper.toDateTime(date));
        return edt;
    }
}
