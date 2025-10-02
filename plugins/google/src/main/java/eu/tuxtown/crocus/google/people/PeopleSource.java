package eu.tuxtown.crocus.google.people;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.*;
import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventSource;
import eu.tuxtown.crocus.google.GoogleAuth;
import eu.tuxtown.crocus.google.request.RequestExecutor;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@NotNullByDefault
public class PeopleSource implements EventSource {

    private final PeopleService service;
    private final RequestExecutor executor;
    private final Function<String, String> formatFunction;

    public PeopleSource(PeopleConfig cfg) {
        try {
            GoogleClientSecrets secrets = GoogleClientSecrets.load(GsonFactory.getDefaultInstance(), new StringReader(cfg.getAuth()));
            Credential credential = GoogleAuth.authorize(secrets);
            this.service = new PeopleService.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), credential)
                    .setApplicationName("Crocus")
                    .build();
            this.executor = RequestExecutor.getDefault(this.service);
            this.formatFunction = cfg.getFormat();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to create google people source", e);
        }
    }

    @Override
    public String name() {
        return "people";
    }

    private PeopleService.People.Connections.List listConnections() throws IOException {
        return this.service.people().connections().list("people/me").setPersonFields("names,birthdays");
    }

    @Override
    public List<Event> retrieveEvents() throws IOException {
        List<Event> events = new ArrayList<>();
        ListConnectionsResponse response = this.executor.execute(this.listConnections());
        while (true) {
            for (Person person : response.getConnections()) {
                String resourceName = person.getResourceName();
                String displayName = findPreferredField(person.getNames(), Name::getMetadata, Name::getDisplayName);
                LocalDate birthDate = findPreferredField(person.getBirthdays(), Birthday::getMetadata, birthday -> {
                    Date date = birthday.getDate();
                    if (date != null && date.getDay() != null && date.getMonth() != null && date.getYear() != null) {
                        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
                    } else {
                        return null;
                    }
                });
                if (resourceName != null && displayName != null && birthDate != null) {
                    Event event = Event.builder(person.getResourceName())
                            .name(this.formatFunction.apply(displayName))
                            .day(birthDate, birthDate)
                            .build();
                    events.add(event);
                }
            }
            if (response.getNextPageToken() == null) break;
            response = this.executor.execute(this.listConnections().setPageToken(response.getNextPageToken()));
        }
        return Collections.unmodifiableList(events);
    }

    @Nullable
    private static <F, T> T findPreferredField(@Nullable List<F> fields, Function<F, FieldMetadata> metadataGetter, Function<F, @Nullable T> extractor) {
        if (fields == null) return null;

        for (F field : fields) {
            FieldMetadata metadata = metadataGetter.apply(field);
            if (metadata.getPrimary() != null && metadata.getPrimary()) {
                T value = extractor.apply(field);
                if (value != null) return value;
            }
        }

        for (F field : fields) {
            T value = extractor.apply(field);
            if (value != null) return value;
        }

        return null;
    }
}
