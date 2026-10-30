package eu.tuxtown.crocus.caldav;

import biweekly.ICalVersion;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.property.Classification;
import biweekly.property.Status;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.github.sardine.impl.SardineException;
import com.github.sardine.impl.SardineImpl;
import eu.tuxtown.crocus.api.calendar.Calendar;
import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventKey;
import eu.tuxtown.crocus.caldav.util.ListEventsReport;
import eu.tuxtown.crocus.caldav.util.ResourceHelper;
import eu.tuxtown.crocus.ical.api.ICalAttributes;
import org.apache.http.Header;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@NotNullByDefault
public class DavCalendar implements Calendar {

    private final URI uri;
    private final Sardine sardine;

    public DavCalendar(DavCalendarConfig cfg) {
        this.uri = ResourceHelper.normalizeURI(cfg.getUri());
        if (cfg.getUsername() != null && cfg.getPassword() != null) {
            this.sardine = SardineFactory.begin(cfg.getUsername(), cfg.getPassword());
        } else {
            this.sardine = SardineFactory.begin();
        }
    }

    @Override
    public String id() {
        return this.uri.toString();
    }

    @Override
    public String name() {
        return "caldav:" + this.uri;
    }

    @Override
    public OptionalInt deleteAllEvents() throws IOException {
        try {
            List<URI> events = this.sardine.report(this.uri.toString(), 1, new ListEventsReport(this.uri));
            for (URI uri : events) {
                this.sardine.delete(uri.toString());
            }
            return OptionalInt.of(events.size());
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    @Override
    public void deleteEvents(Set<String> events) throws IOException {
        for (String uid : events) {
            URI resource = this.uri.resolve(uid + ".ics");
            try {
                this.sardine.delete(resource.toString());
            } catch (SardineException e) {
                if (e.getStatusCode() != 404 && e.getStatusCode() != 410) {
                    throw e;
                }
            }
        }
    }

    @Override
    public void updateEvents(CalendarData data, CalendarIds ids) throws IOException {
        for (EventKey eventKey : data.deletedEvents()) {
            String uid = ids.getId(eventKey);
            if (uid != null) {
                URI resource = this.uri.resolve(uid + ".ics");
                this.sardine.delete(resource.toString());
            }
        }
        for (Map.Entry<EventKey, Event> entry : data.events().entrySet()) {
            EventKey eventKey = entry.getKey();
            String uid = ids.getId(eventKey);
            List<Header> headers = new ArrayList<>();
            headers.add(new BasicHeader(HTTP.EXPECT_DIRECTIVE, HTTP.EXPECT_CONTINUE));
            headers.add(new BasicHeader(HTTP.CONTENT_TYPE, "text/calendar;charset=utf-8"));

            if (uid == null) {
                uid = UUID.randomUUID().toString();
                ids.setId(eventKey, uid);
                headers.add(new BasicHeader("If-None-Match", "*"));
            } else {
                headers.add(new BasicHeader("If-Match", "*"));
            }

            URI resource = this.uri.resolve(uid + ".ics");
            byte[] bytes = this.createICalEvent(entry.getValue(), uid).getBytes(StandardCharsets.UTF_8);
            ((SardineImpl) this.sardine).put(resource.toString(), new ByteArrayEntity(bytes), headers);
        }
    }

    private String createICalEvent(Event event, String uid) {
        VEvent ical = new VEvent();
        ical.setUid(uid);
        ical.setDateTimeStamp(new Date(Instant.now().toEpochMilli()));
        ical.setSummary(event.name());
        event.description().ifPresent(ical::setDescription);
        event.location().ifPresent(ical::setLocation);
        event.url().ifPresent(uri -> ical.setUrl(uri.toString()));

        switch (event.time()) {
            case Event.EventTime.AllDay allDay -> {
                ZoneId zone = java.util.Calendar.getInstance().getTimeZone().toZoneId();
                ical.setDateStart(new Date(ZonedDateTime.of(allDay.start(), LocalTime.MIDNIGHT, zone).toInstant().toEpochMilli()), false);
                ical.setDateEnd(new Date(ZonedDateTime.of(allDay.end().plusDays(1), LocalTime.MIDNIGHT, zone).toInstant().toEpochMilli()), false);
            }
            case Event.EventTime.OpenEnd openEnd -> {
                ical.setDateStart(new Date(openEnd.start().toEpochMilli()), true);
            }
            case Event.EventTime.Timed timed -> {
                ical.setDateStart(new Date(timed.start().toEpochMilli()), true);
                ical.setDateEnd(new Date(timed.end().toEpochMilli()), true);
            }
        }

        event.attribute(ICalAttributes.PRIORITY).filter(prio -> prio != 0).ifPresent(ical::setPriority);
        event.attribute(ICalAttributes.STATUS).map(Status::new).filter(status -> status.validate(List.of(), ICalVersion.V2_0).isEmpty()).ifPresent(ical::setStatus);
        event.attribute(ICalAttributes.COLOR).ifPresent(ical::setColor);
        event.attribute(ICalAttributes.CLASSIFICATION).map(Classification::new).filter(classification -> classification.validate(List.of(), ICalVersion.V2_0).isEmpty()).ifPresent(ical::setClassification);

        ICalendar calendar = new ICalendar();
        calendar.addEvent(ical);
        return calendar.write();
    }
}
