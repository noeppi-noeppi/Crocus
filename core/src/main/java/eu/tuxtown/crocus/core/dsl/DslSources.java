package eu.tuxtown.crocus.core.dsl;

import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.calendar.Calendar;
import eu.tuxtown.crocus.api.calendar.EventFilter;
import eu.tuxtown.crocus.api.calendar.EventSource;
import eu.tuxtown.crocus.api.service.CalendarType;
import eu.tuxtown.crocus.api.service.EventFilterType;
import eu.tuxtown.crocus.api.service.EventSourceType;
import eu.tuxtown.crocus.core.configuration.ConfiguredEventSource;
import eu.tuxtown.crocus.core.configuration.ConfiguredService;
import eu.tuxtown.crocus.core.loader.Services;
import groovy.lang.Closure;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@NotNullByDefault
public class DslSources extends ServiceContainer<EventSourceType<?, ?>> {

    private final Services services;
    private final Map<String, ConfiguredService<CalendarType<?, ?>, Calendar>> calendars;
    private final Map<String, ConfiguredService<EventSourceType<?, ?>, ConfiguredEventSource>> sources;

    public DslSources(Services services, Map<String, ConfiguredService<CalendarType<?, ?>, Calendar>> calendars, Map<String, ConfiguredService<EventSourceType<?, ?>, ConfiguredEventSource>> sources) {
        super(services, Crocus.ServiceDomain.EVENT_SOURCE);
        this.services = services;
        this.calendars = calendars;
        this.sources = sources;
    }

    @Override
    protected Object addService(Services.Service<EventSourceType<?, ?>> service, String name, Closure<?> config) {
        DslSource dslSource = new DslSource();
        config.setDelegate(dslSource);
        config.setResolveStrategy(Closure.DELEGATE_FIRST);
        config.call();

        if (dslSource.recipients.isEmpty()) {
            return Void.class;
        }

        if (this.sources.containsKey(name)) {
            throw new IllegalStateException("Duplicate event source: " + name);
        }

        EventSource source = ScriptEngine.configure(service.instance(), Objects.requireNonNullElse(dslSource.cfg, Closure.IDENTITY));
        this.sources.put(name, new ConfiguredService<>(service, name, new ConfiguredEventSource(source, dslSource.filterChain, dslSource.recipients)));
        return Void.class;
    }

    public class DslSource {

        private @Nullable Closure<?> cfg;
        private final List<EventFilter> filterChain;
        private final Set<String> recipientIds;
        private final List<ConfiguredService<CalendarType<?, ?>, Calendar>> recipients;

        public DslSource() {
            this.cfg = null;
            this.filterChain = new ArrayList<>();
            this.recipientIds = new HashSet<>();
            this.recipients = new ArrayList<>();
        }

        public void configure(Closure<?> cfg) {
            if (this.cfg != null) throw new IllegalStateException("Source has already been configured");
            this.cfg = cfg;
        }

        public void filter(String typeId, Closure<?> cfg) {
            Services.Service<EventFilterType<?, ?>> service = DslSources.this.services.resolve(Crocus.ServiceDomain.EVENT_FILTER, typeId);
            this.filterChain.add(ScriptEngine.configure(service.instance(), cfg));
        }

        public void into(String calendarName) {
            ConfiguredService<CalendarType<?, ?>, Calendar> calendar = DslSources.this.calendars.get(calendarName);
            if (calendar == null) throw new NoSuchElementException("No such calendar: " + calendarName);
            if (this.recipientIds.add(calendarName)) this.recipients.add(calendar);
        }
    }
}
