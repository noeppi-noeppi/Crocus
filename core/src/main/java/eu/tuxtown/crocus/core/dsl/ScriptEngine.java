package eu.tuxtown.crocus.core.dsl;

import bootstrap.api.ModuleSystem;
import eu.tuxtown.crocus.api.calendar.Calendar;
import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.delegate.DelegateConfigurable;
import eu.tuxtown.crocus.api.service.CalendarType;
import eu.tuxtown.crocus.api.service.EventSourceType;
import eu.tuxtown.crocus.core.configuration.ConfiguredEventSource;
import eu.tuxtown.crocus.core.configuration.ConfiguredService;
import eu.tuxtown.crocus.core.configuration.SystemConfiguration;
import eu.tuxtown.crocus.core.dsl.meta.metaclass.DslMetaClass;
import eu.tuxtown.crocus.core.dsl.meta.metaclass.EventTimeMetaClass;
import eu.tuxtown.crocus.core.loader.Services;
import groovy.lang.*;
import groovy.util.DelegatingScript;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@NotNullByDefault
public class ScriptEngine {

    private static boolean initialised = false;

    public static synchronized void init(ModuleSystem system, ModuleLayer.Controller pluginController) {
        if (!initialised) {
            // groovy uses heavy reflection on everything an that is okay
            Module groovyModule = GroovySystem.class.getModule();
            for (Module bootstrapModule : system.layer().modules()) {
                for (String pkg : bootstrapModule.getPackages()) {
                    system.addOpens(bootstrapModule, pkg, groovyModule);
                }
            }
            for (Module pluginModule : pluginController.layer().modules()) {
                for (String pkg : pluginModule.getPackages()) {
                    pluginController.addOpens(pluginModule, pkg, groovyModule);
                }
            }

            MetaClassRegistry reg = GroovySystem.getMetaClassRegistry();
            reg.setMetaClass(Event.EventTime.class, new EventTimeMetaClass(reg, Event.EventTime.class));
            reg.setMetaClass(Event.EventTime.OpenEnd.class, new EventTimeMetaClass(reg, Event.EventTime.OpenEnd.class));
            reg.setMetaClass(Event.EventTime.Timed.class, new EventTimeMetaClass(reg, Event.EventTime.Timed.class));
            reg.setMetaClass(Event.EventTime.AllDay.class, new EventTimeMetaClass(reg, Event.EventTime.AllDay.class));
            DslMetaClass.applyToClass(reg, Instant.class);
            DslMetaClass.applyToClass(reg, ZonedDateTime.class);
            DslMetaClass.applyToClass(reg, LocalDateTime.class);
            DslMetaClass.applyToClass(reg, LocalDate.class);
            initialised = true;
        }
    }

    private static synchronized void checkInit() {
        if (!initialised) {
            throw new IllegalStateException("The script engine has not yet been initialized");
        }
    }

    public static SystemConfiguration loadConfig(Path path, Services services, Properties secrets) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new FileNotFoundException("System configuration not found: " + path.toAbsolutePath().normalize());
        }

        checkInit();
        Dsl dsl = new Dsl(services, secrets);

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            loadDsl(reader, dsl, null);
        }

        Map<ConfiguredService.Identifier, String> calendarKeys = new HashMap<>();
        for (Map.Entry<String, ConfiguredService<CalendarType<?, ?>, Calendar>> entry : dsl.calendars.entrySet()) {
            String calendarName = entry.getKey();
            ConfiguredService.Identifier ident = entry.getValue().identifier();
            if (!calendarKeys.containsKey(ident)) {
                calendarKeys.put(ident, calendarName);
            } else {
                throw new IllegalStateException("The calendars " + calendarKeys.get(ident) + " and " + calendarName + " reference the same downstream calendar.");
            }
        }

        return new SystemConfiguration(Map.copyOf(dsl.calendars), Map.copyOf(dsl.sources));
    }

    private static void loadDsl(Reader reader, Object delegate, @Nullable MetaClass metaClass) {
        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        compilerConfig.setScriptBaseClass(DelegatingScript.class.getName());
        GroovyShell shell = new GroovyShell(new Binding(), compilerConfig);
        DelegatingScript script;
        script = (DelegatingScript) shell.parse(reader);
        script.setDelegate(delegate);
        if (metaClass != null) {
            try {
                Field field = DelegatingScript.class.getDeclaredField("metaClass");
                field.setAccessible(true);
                field.set(script, metaClass);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("Failed to inject script metaclass", e);
            }
        }
        script.run();
    }

    public static <T, D> T configure(DelegateConfigurable<T, D> configurable, Closure<?> config) {
        checkInit();
        D delegate = configurable.createDelegate();
        DslMetaClass.applyTo(delegate);
        config.setDelegate(delegate);
        config.setResolveStrategy(Closure.DELEGATE_FIRST);
        config.call();
        return configurable.create(delegate);
    }

    public static <T, D> T configureFromString(DelegateConfigurable<T, D> configurable, String config) {
        checkInit();
        D delegate = configurable.createDelegate();
        DslMetaClass.applyTo(delegate);
        try (Reader reader = new StringReader(config)) {
            loadDsl(reader, delegate, DefaultGroovyMethods.getMetaClass(delegate));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return configurable.create(delegate);
    }

    public static class Dsl extends Globals {

        private final Services services;
        private final DslSecrets secrets;
        private final Map<String, ConfiguredService<CalendarType<?, ?>, Calendar>> calendars;
        private final Map<String, ConfiguredService<EventSourceType<?, ?>, ConfiguredEventSource>> sources;

        private Dsl(Services services, Properties secrets) {
            this.services = services;
            this.secrets = new DslSecrets(secrets);
            this.calendars = new HashMap<>();
            this.sources = new HashMap<>();
        }

        public DslSecrets getSecrets() {
            return this.secrets;
        }

        public void calendars(@DelegatesTo(value = DslCalendars.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            DslCalendars calendars = new DslCalendars(this.services, this.calendars);
            closure.setDelegate(calendars);
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.call();
        }

        public void sources(@DelegatesTo(value = DslSources.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            DslSources sources = new DslSources(this.services, Collections.unmodifiableMap(this.calendars), this.sources);
            closure.setDelegate(sources);
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.call();
        }
    }
}
