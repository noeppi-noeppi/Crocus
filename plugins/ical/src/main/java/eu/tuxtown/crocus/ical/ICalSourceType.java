package eu.tuxtown.crocus.ical;

import eu.tuxtown.crocus.api.service.EventSourceType;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public class ICalSourceType implements EventSourceType<ICalSource, ICalConfig> {

    @Override
    public String name() {
        return "ical";
    }

    @Override
    public ICalConfig createDelegate() {
        return new ICalConfig();
    }

    @Override
    public ICalSource create(ICalConfig delegate) {
        return new ICalSource(delegate);
    }
}
