package eu.tuxtown.crocus.google.calendar;

import eu.tuxtown.crocus.api.service.CalendarType;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public class GoogleCalendarType implements CalendarType<GoogleCalendar, GoogleCalendarConfig> {

    @Override
    public String name() {
        return "google";
    }

    @Override
    public GoogleCalendarConfig createDelegate() {
        return new GoogleCalendarConfig();
    }

    @Override
    public GoogleCalendar create(GoogleCalendarConfig delegate) {
        return new GoogleCalendar(delegate);
    }
}
