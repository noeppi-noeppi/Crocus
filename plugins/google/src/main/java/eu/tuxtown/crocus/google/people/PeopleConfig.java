package eu.tuxtown.crocus.google.people;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.function.Function;

@NotNullByDefault
public class PeopleConfig {

    @Nullable private String auth;
    private Function<String, String> formatFunction = name -> "Birthday of " + name;

    public void auth(String auth) {
        this.auth = auth;
    }

    public void format(String formatString) {
        this.formatFunction = name -> String.format(formatString, name);
    }

    public void format(Function<String, String> format) {
        this.formatFunction = format;
    }

    public String getAuth() {
        if (this.auth == null) throw new NoSuchElementException("Google people source has no auth properties set");
        return this.auth;
    }

    public Function<String, String> getFormat() {
        return this.formatFunction;
    }
}
