import eu.tuxtown.crocus.api.service.CalendarType;
import eu.tuxtown.crocus.caldav.DavCalendarType;

module tuxtown.crocus.caldav {
    requires static org.jetbrains.annotations;

    requires tuxtown.crocus.core;
    requires tuxtown.crocus.ical;
    requires com.github.sardine;
    requires net.sf.biweekly;
    requires org.apache.httpcomponents.httpcore;

    provides CalendarType with DavCalendarType;
}
