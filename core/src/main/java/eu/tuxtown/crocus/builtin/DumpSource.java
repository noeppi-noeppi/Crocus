package eu.tuxtown.crocus.builtin;

import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventSource;
import eu.tuxtown.crocus.api.resource.Resource;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;

@NotNullByDefault
public class DumpSource implements EventSource {

    private final Resource res;
    private final Charset charset;

    public DumpSource(DumpConfig cfg) {
        this.res = cfg.getSource();
        this.charset = cfg.getCharset();
    }

    @Override
    public String name() {
        return "dump:" + this.res;
    }

    @Override
    public List<Event> retrieveEvents() throws IOException {
        try (Reader reader = this.res.openReader(this.charset)) {
            return Crocus.loadEvents(reader);
        }
    }
}
