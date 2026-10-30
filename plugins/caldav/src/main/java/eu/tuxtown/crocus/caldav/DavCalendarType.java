package eu.tuxtown.crocus.caldav;

import eu.tuxtown.crocus.api.service.CalendarType;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public class DavCalendarType implements CalendarType<DavCalendar, DavCalendarConfig> {

    @Override
    public String name() {
        return "caldav";
    }

    @Override
    public DavCalendarConfig createDelegate() {
        return new DavCalendarConfig();
    }

    @Override
    public DavCalendar create(DavCalendarConfig delegate) {
        return new DavCalendar(delegate);
    }
}
