package eu.tuxtown.crocus.api;

import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.service.CalendarType;
import eu.tuxtown.crocus.api.service.EventFilterType;
import eu.tuxtown.crocus.api.service.EventSourceType;
import eu.tuxtown.crocus.api.service.Nameable;
import eu.tuxtown.crocus.core.CrocusRuntime;
import eu.tuxtown.crocus.core.dsl.meta.metaclass.DslMetaClass;
import eu.tuxtown.crocus.core.loader.Services;
import eu.tuxtown.crocus.impl.json.EventJson;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The Crocus API instance.
 */
@NotNullByDefault
public class Crocus {

    private static final StackWalker STACK = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private Crocus() {}

    /**
     * Log an info message.
     */
    public static void info(String message) {
        CrocusRuntime runtime = CrocusRuntime.get();
        if (runtime.verbose()) {
            Class<?> caller = getCallerClass();
            runtime.logWithCaller(caller, message, false);
        } else {
            runtime.log(message, false);
        }
    }

    /**
     * Log a debug message, which is only printed in verbose mode.
     */
    public static void debug(String message) {
        CrocusRuntime runtime = CrocusRuntime.get();
        if (runtime.verbose()) {
            Class<?> caller = getCallerClass();
            runtime.logWithCaller(caller, message, true);
        } else {
            runtime.log(message, true);
        }
    }

    /**
     * If called from a plugin, gets a path in which the plugin is free to store whatever
     * data it needs to. Calling this from outside a plugin will fail.
     */
    public static Path pluginPath(Location location) throws IOException {
        Class<?> caller = getCallerClass();
        if (caller == null || !caller.getModule().isNamed() || caller.getModule().getLayer() != CrocusRuntime.get().pluginLayer()) {
            throw new IllegalStateException("Can't get plugin directory: No valid caller found");
        }

        Path basePath = switch (location) {
            case PUBLIC -> CrocusRuntime.get().path().resolve("plugin-data");
            case SECRET -> CrocusRuntime.get().path().resolve("secret-data");
        };

        Path path = basePath.resolve(caller.getModule().getName()).normalize();
        if (!Files.isDirectory(path)) {
            Files.createDirectories(path);
            if (location == Location.SECRET) try {
                PosixFileAttributeView posixView = Files.getFileAttributeView(path, PosixFileAttributeView.class);
                PosixFileAttributes posixAttr = posixView.readAttributes();
                posixView.setPermissions(posixAttr.permissions().stream()
                        .filter(perm -> perm == PosixFilePermission.OWNER_READ || perm == PosixFilePermission.OWNER_WRITE || perm == PosixFilePermission.OWNER_EXECUTE)
                        .collect(Collectors.toUnmodifiableSet())
                );
            } catch (IOException e) {
                //
            }
        }
        return path;
    }

    private static @Nullable Class<?> getCallerClass() {
        return STACK.walk(frames -> frames
                .map(StackWalker.StackFrame::getDeclaringClass)
                .skip(2).findFirst()).orElse(null);
    }

    /**
     * Parse a UUID from a string. This is more lenient that {@link UUID#fromString(String)}
     * as it accepts string without dashes.
     */
    public static UUID parseUUID(String data) {
        try {
            return UUID.fromString(data);
        } catch (Exception e) {
            try {
                BigInteger value = new BigInteger(data.replace("-", ""), 16);
                return new UUID(value.shiftRight(64).longValue(), value.longValue());
            } catch (Exception x) {
                e.addSuppressed(x);
                throw e;
            }
        }
    }

    /**
     * Reads an event list from the internal crocus format.
     */
    public static List<Event> loadEvents(Reader reader) throws IOException {
        return EventJson.readEvents(reader);
    }

    /**
     * Gets the service instance constructed for a given service class under a service domain.
     *
     * @throws IllegalStateException if called during service loading.
     */
    public static <S extends Nameable, T extends S> T serviceInstance(ServiceDomain<S> domain, Class<T> serviceClass) {
        Services.Service<S> service = CrocusRuntime.get().services().get(domain, serviceClass);
        return serviceClass.cast(service.instance());
    }

    /**
     * Mark an object as a DSL-object. DSL objects receive type transformations and access to globals.
     * Delegates are automatically marked as dsl objects.
     */
    public static void makeDslObject(Object object) {
        DslMetaClass.applyTo(object);
    }

    /**
     * A type of {@link #pluginPath(Location) plugin path}.
     */
    @NotNullByDefault
    public enum Location {
        /**
         * Path for general data.
         */
        PUBLIC,

        /**
         * Path for data that contains secrets.
         */
        SECRET
    }

    /**
     * A service domain in which Crocus loads services from plugins.
     *
     * @param <T> The service type base class.
     */
    @NotNullByDefault
    public static class ServiceDomain<T extends Nameable> {

        public static final ServiceDomain<CalendarType<?, ?>> CALENDAR = new ServiceDomain<>(CalendarType.class);
        public static final ServiceDomain<EventFilterType<?, ?>> EVENT_FILTER = new ServiceDomain<>(EventFilterType.class);
        public static final ServiceDomain<EventSourceType<?, ?>> EVENT_SOURCE = new ServiceDomain<>(EventSourceType.class);

        private final Class<T> baseClass;

        private ServiceDomain(Class<?> baseClass) {
            //noinspection unchecked
            this.baseClass = (Class<T>) baseClass;
        }

        /**
         * Gets the base class for service types on this service domain.
         */
        public Class<T> getTypeClass() {
            return this.baseClass;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return other instanceof ServiceDomain<?> domain && this.baseClass == domain.baseClass;
        }

        @Override
        public int hashCode() {
            return this.baseClass.hashCode();
        }
    }
}
