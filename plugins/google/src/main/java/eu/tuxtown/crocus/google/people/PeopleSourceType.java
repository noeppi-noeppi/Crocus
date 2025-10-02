package eu.tuxtown.crocus.google.people;

import eu.tuxtown.crocus.api.service.EventSourceType;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public class PeopleSourceType implements EventSourceType<PeopleSource, PeopleConfig> {

    @Override
    public String name() {
        return "people";
    }

    @Override
    public PeopleConfig createDelegate() {
        return new PeopleConfig();
    }

    @Override
    public PeopleSource create(PeopleConfig delegate) {
        return new PeopleSource(delegate);
    }
}
