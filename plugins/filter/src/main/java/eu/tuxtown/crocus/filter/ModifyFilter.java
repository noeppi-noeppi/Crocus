package eu.tuxtown.crocus.filter;

import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventFilter;
import eu.tuxtown.crocus.api.calendar.EventKey;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

@NotNullByDefault
public class ModifyFilter implements EventFilter {

    private final List<SimpleFilter> when;
    private final UnaryOperator<String> name;
    private final UnaryOperator<String> description;
    private final UnaryOperator<String> location;
    private final UnaryOperator<URI> url;

    public ModifyFilter(ModifyFilterConfig cfg) {
        this.when = List.copyOf(cfg.getWhen());
        this.name = cfg.getName();
        this.description = cfg.getDescription();
        this.location = cfg.getLocation();
        this.url = cfg.getUrl();
    }

    @Override
    public String name() {
        return "modify";
    }

    @Override
    public @Nullable Event filter(EventKey key, Event event) {
        for (SimpleFilter when : this.when) {
            if (!when.test(event)) return event;
        }
        String newName = this.name.apply(event.name());
        String newDescription = this.description.apply(event.description().orElse(""));
        String newLocation = this.location.apply(event.location().orElse(""));
        URI newURL = this.url.apply(event.url().orElse(URI.create("")));
        if (!Objects.equals(event.name(), newName) || !Objects.equals(event.description().orElse(""), newDescription)
                || !Objects.equals(event.location().orElse(""), newLocation) || !Objects.equals(event.url().orElse(URI.create("")), newURL)) {
            return Event.builder(event)
                    .name(newName)
                    .description(newDescription)
                    .location(newLocation)
                    .url(newURL)
                    .build();
        } else {
            return event;
        }
    }
}
