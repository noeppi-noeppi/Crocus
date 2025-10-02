package eu.tuxtown.crocus.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.people.v1.PeopleServiceScopes;
import eu.tuxtown.crocus.api.Crocus;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NotNullByDefault
public class GoogleAuth {

    private static final Map<String, Credential> credentialsByClientId = new HashMap<>();
    private static final List<String> SCOPES = List.of(
            CalendarScopes.CALENDAR_READONLY,
            CalendarScopes.CALENDAR_EVENTS,
            PeopleServiceScopes.CONTACTS_READONLY
    );

    public static Credential authorize(GoogleClientSecrets secrets) throws GeneralSecurityException, IOException {
        if (secrets.getInstalled() == null || secrets.getInstalled().getClientId() == null) {
            throw new IllegalArgumentException("Invalid google secrets");
        }
        String clientId = secrets.getInstalled().getClientId();
        if (credentialsByClientId.containsKey(clientId)) {
            return credentialsByClientId.get(clientId);
        }

        Path pluginPath = Crocus.pluginPath(Crocus.Location.SECRET);
        Files.createDirectories(pluginPath);
        Path credentialPath = pluginPath.resolve("auth-" + clientId);

        HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(transport, GsonFactory.getDefaultInstance(), secrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(credentialPath.toFile()))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        credentialsByClientId.put(clientId, credential);
        return credential;
    }
}
