package eu.tuxtown.crocus.caldav;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.NoSuchElementException;

@NotNullByDefault
public class DavCalendarConfig {

    private @Nullable URI uri;
    private @Nullable String username;
    private @Nullable String password;

    public void uri(URI uri) {
        this.uri = uri;
    }

    public void authenticate(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public URI getUri() {
        if (this.uri == null) throw new NoSuchElementException("No CalDAV URI set.");
        return this.uri;
    }

    public @Nullable String getUsername() {
        return this.username;
    }

    public @Nullable String getPassword() {
        return this.password;
    }
}
