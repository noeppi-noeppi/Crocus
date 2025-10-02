package eu.tuxtown.crocus.frab;

import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventSource;
import eu.tuxtown.crocus.api.resource.Resource;
import eu.tuxtown.crocus.frab.api.FrabAttributes;
import eu.tuxtown.crocus.frab.model.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@NotNullByDefault
public class FrabSource implements EventSource {

    private final Resource res;
    private final Charset charset;
    private final ZoneId timezone;

    public FrabSource(FrabConfig cfg) {
        this.res = cfg.getSource();
        this.charset = cfg.getCharset();
        this.timezone = cfg.getTimezone();
    }

    @Override
    public String name() {
        return "frab:" + this.res;
    }

    @Override
    public List<Event> retrieveEvents() throws IOException {
        Schedule schedule;
        ClassLoader ctx = Thread.currentThread().getContextClassLoader();
        try (Reader reader = this.res.openReader(this.charset)) {
            Thread.currentThread().setContextClassLoader(FrabSource.class.getClassLoader());
            Unmarshaller unmarshaller = JAXBContext.newInstance(ObjectFactory.class).createUnmarshaller();
            Object object = unmarshaller.unmarshal(reader);
            if (object instanceof JAXBElement<?> jaxb) {
                schedule = (Schedule) jaxb.getValue();
            } else {
                schedule = (Schedule) object;
            }
        } catch (JAXBException e) {
            throw new IOException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(ctx);
        }

        List<Event> events = new ArrayList<>();
        String title = schedule.getConference().getTitle().strip();
        ZoneId conferenceTimeZone = Objects.requireNonNullElse(schedule.getConference().getTimeZoneName(), this.timezone);
        @Nullable URI baseURL = schedule.getConference().getBaseUrl();
        for (ConferenceDay day : schedule.getDay()) {
            for (ConferenceRoom room : day.getRoom()) {
                String roomName = room.getName().strip();
                for (ConferenceEvent event : room.getEvent()) {
                    Event.Builder builder = Event.builder(Crocus.parseUUID(event.getGuid()));

                    String name = event.getTitle().strip();
                    if (event.getSubtitle() != null && !event.getSubtitle().isBlank()) {
                        name += " - " + event.getSubtitle().strip();
                    }
                    builder.name(name);

                    String description = event.getAbstract() == null ? "" : event.getAbstract().strip();
                    if (event.getPersons() != null && !event.getPersons().getPerson().isEmpty()) {
                        List<String> people = List.copyOf(event.getPersons().getPerson());
                        builder.attribute(FrabAttributes.PEOPLE, people);
                        description = ("(" + String.join(", ", people) + ")" + "\n\n" + description).strip();
                    }
                    if (event.getDescription() != null && !event.getDescription().isBlank()) {
                        if (!description.isEmpty()) description += "\n\n";
                        description += event.getDescription().strip();
                    }
                    if (!description.isEmpty()) builder.description(description);

                    builder.location(roomName + " - " + title);
                    if (event.getUrl() != null && !event.getUrl().toString().isEmpty()) {
                        URI finalURL = baseURL != null ? baseURL.resolve(event.getUrl()) : event.getUrl();
                        builder.url(finalURL);
                    }

                    Instant start = event.getDate() != null ? event.getDate().toInstant() : ZonedDateTime.of(day.getDate(), event.getStart(), conferenceTimeZone).toInstant();
                    Duration duration = event.getDuration();
                    builder.time(start, start.plus(duration));

                    builder.attribute(FrabAttributes.ROOM, roomName);
                    if (event.getType() != null && !event.getType().isBlank()) builder.attribute(FrabAttributes.TYPE, event.getType().strip());

                    events.add(builder.build());
                }
            }
        }

        return Collections.unmodifiableList(events);
    }
}
