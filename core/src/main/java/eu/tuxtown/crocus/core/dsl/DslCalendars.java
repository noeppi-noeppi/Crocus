package eu.tuxtown.crocus.core.dsl;

import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.calendar.Calendar;
import eu.tuxtown.crocus.api.service.CalendarType;
import eu.tuxtown.crocus.core.configuration.ConfiguredService;
import eu.tuxtown.crocus.core.loader.Services;
import groovy.lang.Closure;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Map;

@NotNullByDefault
public class DslCalendars extends ServiceContainer<CalendarType<?, ?>> {

    private final Map<String, ConfiguredService<CalendarType<?, ?>, Calendar>> calendars;

    public DslCalendars(Services services, Map<String, ConfiguredService<CalendarType<?, ?>, Calendar>> calendars) {
        super(services, Crocus.ServiceDomain.CALENDAR);
        this.calendars = calendars;
    }

    @Override
    protected Object addService(Services.Service<CalendarType<?, ?>> service, String name, Closure<?> config) {
        if (this.calendars.containsKey(name)) {
            throw new IllegalStateException("Duplicate calendar name: " + name);
        }
        Calendar calendar = ScriptEngine.configure(service.instance(), config);
        this.calendars.put(name, new ConfiguredService<>(service, calendar.id(), calendar));
        return Void.class;
    }
}
