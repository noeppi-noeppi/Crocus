package eu.tuxtown.crocus.google.calendar;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

@NotNullByDefault
public class GoogleCalendarConfig {

    @Nullable private String auth;
    @Nullable private String calendarId;
    private boolean isBirthdayCalendar;

    public GoogleCalendarConfig() {

    }

    public void auth(String auth) {
        this.auth = auth;
    }

    public void calendarId(String calendarId) {
        this.calendarId = calendarId;
        this.isBirthdayCalendar = false;
    }

    public void birthdayCalendarId(String calendarId) {
        this.calendarId = calendarId;
        this.isBirthdayCalendar = true;
    }

    public String getAuth() {
        if (this.auth == null) throw new NoSuchElementException("Google calendar has no auth properties set");
        return this.auth;
    }

    public String getCalendarId() {
        if (this.calendarId == null) throw new NoSuchElementException("Google calendar has no calendar id set");
        return this.calendarId;
    }

    public boolean isBirthdays() {
        return this.isBirthdayCalendar;
    }
}
