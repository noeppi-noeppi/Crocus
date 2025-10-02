package eu.tuxtown.crocus.core.dsl.meta;

import eu.tuxtown.crocus.api.attribute.Attribute;
import eu.tuxtown.crocus.api.attribute.Attributes;
import eu.tuxtown.crocus.api.attribute.DefaultedAttribute;
import eu.tuxtown.crocus.api.delegate.DelegateConfiguration;
import eu.tuxtown.crocus.api.resource.Resource;
import eu.tuxtown.crocus.core.CrocusRuntime;
import eu.tuxtown.crocus.impl.resource.MemoryResource;
import eu.tuxtown.crocus.impl.resource.PathResource;
import eu.tuxtown.crocus.impl.resource.UrlResource;
import eu.tuxtown.crocus.impl.time.AnyTemporalAmount;
import groovy.lang.Closure;
import org.codehaus.groovy.reflection.CachedClass;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.codehaus.groovy.runtime.typehandling.GroovyCastException;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.*;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@NotNullByDefault
public class DslTypeTransformation {

    private static final DateTimeFormatter DATE_FORMAT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .optionalStart()
            .appendLiteral("T")
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .optionalStart()
            .appendOffsetId()
            .optionalStart()
            .appendLiteral('[')
            .parseCaseSensitive()
            .appendZoneRegionId()
            .appendLiteral(']')
            .toFormatter(Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());

    private static final Pattern PERIOD_PATTERN_START = Pattern.compile("^\\s*([-+]?)\\s*(\\d+)\\s*(\\w+)\\s*(.*?)\\s*$");
    private static final Pattern PERIOD_PATTERN_CONT = Pattern.compile("^\\s*([-+])\\s*(\\d+)\\s*(\\w+)\\s*(.*?)\\s*$");

    public static @Nullable Object @Nullable [] castArguments(@Nullable Object[] objects, CachedClass[] types) {
        if (types.length == 0) return objects;
        @Nullable Object[] newObjects = new Object[objects.length];
        for (int i = 0; i < objects.length; i++) {
            Class<?> type = i < types.length ? types[i].getTheClass() : types[types.length - 1].getTheClass();
            if (objects[i] == null) {
                newObjects[i] = null;
            } else {
                newObjects[i] = castToType(objects[i], type);
                if (newObjects[i] == null) return null;
            }
        }
        return newObjects;
    }

    public static @Nullable Object castToType(@Nullable Object object, Class<?> type) {
        if (object == null) return DefaultTypeTransformation.castToType(null, type);
        if (type.isAssignableFrom(object.getClass())) return object;

        if (type == DelegateConfiguration.class) {
            DelegateConfiguration delegateConfig = castToDelegateConfiguration(object);
            if (delegateConfig != null) return delegateConfig;
        }
        if (type == Attribute.class) {
            Attribute<?> attribute = castToAttribute(object);
            if (attribute != null) return attribute;
        }
        if (type == DefaultedAttribute.class) {
            Attribute<?> attribute = castToAttribute(object);
            if (attribute instanceof DefaultedAttribute<?>) return attribute;
        }
        if (type == Instant.class) {
            Instant instant = castToInstant(object);
            if (instant != null) return instant;
        }
        if (type == ZonedDateTime.class || type == ChronoZonedDateTime.class) {
            ZonedDateTime zdt = castToZonedDateTime(object);
            if (zdt != null) return zdt;
        }
        if (type == OffsetDateTime.class) {
            OffsetDateTime odt = castToOffsetDateTime(object);
            if (odt != null) return odt;
        }
        if (type == LocalDateTime.class || type == ChronoLocalDateTime.class) {
            LocalDateTime ldt = castToLocalDateTime(object);
            if (ldt != null) return ldt;
        }
        if (type == LocalDate.class || type == ChronoLocalDate.class) {
            LocalDate ldt = castToLocalDate(object);
            if (ldt != null) return ldt;
        }
        if (type == ZoneId.class) {
            ZoneId zone = castToZoneId(object);
            if (zone != null) return zone;
        }
        if (type == TemporalAmount.class) {
            TemporalAmount temporalAmount = castToTemporalAmount(object);
            if (temporalAmount != null) return temporalAmount;
        }
        if (type == Period.class) {
            Period period = castToPeriod(object);
            if (period != null) return period;
        }
        if (type == Duration.class) {
            Duration duration = castToDuration(object);
            if (duration != null) return duration;
        }
        if (type == DayOfWeek.class) {
            DayOfWeek dow = castToDayOfWeek(object);
            if (dow != null) return dow;
        }
        if (type == Month.class) {
            Month month = castToMonth(object);
            if (month != null) return month;
        }
        if (type == Path.class) {
            Path path = castToPath(object);
            if (path != null) return path;
        }
        if (type == File.class) {
            File file = castToFile(object);
            if (file != null) return file;
        }
        if (type == URI.class) {
            URI uri = castToURI(object);
            if (uri != null) return uri;
        }
        if (type == URL.class) {
            URL url = castToURL(object);
            if (url != null) return url;
        }
        if (type == Resource.class) {
            Resource resource = castToResource(object);
            if (resource != null) return resource;
        }
        if (type == Charset.class) {
            Charset charset = castToCharset(object);
            if (charset != null) return charset;
        }
        if (type.isEnum()) {
            Object enumConstant = castToEnum(object, type);
            if (enumConstant != null) return enumConstant;
        }

        return DefaultTypeTransformation.castToType(object, type);
    }

    private static @Nullable DelegateConfiguration castToDelegateConfiguration(@Nullable Object object) {
        if (object instanceof Closure<?> closure) {
            return new DelegateConfigurationImpl(closure);
        }
        return null;
    }

    private static @Nullable Attribute<?> castToAttribute(@Nullable Object object) {
        if (object instanceof String str) {
            Optional<Attribute<?>> attr = Attributes.get(str);
            if (attr.isEmpty()) throw new GroovyCastException(str, Attribute.class, new NoSuchElementException("No attribute: " + str));
            return attr.get();
        }
        return null;
    }

    private record ZonedInstant(Instant instant, ZoneId zone) {} // For when the instant is too large for a ZDT.
    private static @Nullable ZonedInstant castToTime(@Nullable Object object) {
        boolean wasStringBefore = false;
        if (object instanceof Date date) {
            return new ZonedInstant(date.toInstant(), ZoneOffset.UTC);
        }
        if (object instanceof String string) {
            object = DATE_FORMAT.parse(string);
            wasStringBefore = true;
        }
        if (object instanceof TemporalAccessor accessor) {
            ZoneId zone = TemporalQueries.zone().queryFrom(accessor);
            if (zone == null) zone = wasStringBefore ? ZoneId.systemDefault() : ZoneOffset.UTC;
            if (accessor.isSupported(ChronoField.INSTANT_SECONDS)) {
                long epochSeconds = accessor.getLong(ChronoField.INSTANT_SECONDS);
                long nano = 0;
                if (accessor.isSupported(ChronoField.NANO_OF_SECOND)) {
                    nano = accessor.getLong(ChronoField.NANO_OF_SECOND);
                }
                return new ZonedInstant(Instant.ofEpochSecond(epochSeconds, nano), zone);
            }
            if (wasStringBefore && accessor.isSupported(ChronoField.EPOCH_DAY)) {
                LocalDate date = LocalDate.ofEpochDay(accessor.getLong(ChronoField.EPOCH_DAY));
                if (zone instanceof ZoneOffset offset) {
                    return new ZonedInstant(LocalDateTime.of(date, LocalTime.MIDNIGHT).toInstant(offset), zone);
                } else {
                    return new ZonedInstant(ZonedDateTime.of(date, LocalTime.MIDNIGHT, zone).toInstant(), zone);
                }
            }
        }
        return null;
    }

    private static @Nullable Instant castToInstant(@Nullable Object object) {
        ZonedInstant time = castToTime(object);
        return time == null ? null : time.instant();
    }

    private static @Nullable ZonedDateTime castToZonedDateTime(@Nullable Object object) {
        ZonedInstant time = castToTime(object);
        return time == null ? null : ZonedDateTime.ofInstant(time.instant(), time.zone());
    }

    private static @Nullable OffsetDateTime castToOffsetDateTime(@Nullable Object object) {
        ZonedInstant time = castToTime(object);
        return time == null ? null : OffsetDateTime.ofInstant(time.instant(), time.zone());
    }

    private static @Nullable LocalDateTime castToLocalDateTime(@Nullable Object object) {
        ZonedDateTime zdt = castToZonedDateTime(object);
        return zdt == null ? null : zdt.toLocalDateTime();
    }

    private static @Nullable LocalDate castToLocalDate(@Nullable Object object) {
        ZonedDateTime zdt = castToZonedDateTime(object);
        return zdt == null ? null : zdt.toLocalDate();
    }

    private static @Nullable ZoneId castToZoneId(@Nullable Object object) {
        if (object instanceof String str) {
            try {
                return ZoneId.of(str);
            } catch (Exception e) {
                //
            }
        }
        return null;
    }

    private static @Nullable TemporalAmount castToTemporalAmount(@Nullable Object object) {
        if (object instanceof String str) {
            AnyTemporalAmount temporalAmount = AnyTemporalAmount.ZERO;
            Matcher matcher = PERIOD_PATTERN_START.matcher(str);
            while (matcher.matches()) {
                long amount = Long.parseLong(("-".equals(matcher.group(1)) ? "-" : "") + matcher.group(2));
                TemporalUnit unit = switch (matcher.group(3).toLowerCase(Locale.ROOT)) {
                    case "nano", "nanos", "nanosecond", "nanoseconds" -> ChronoUnit.NANOS;
                    case "micro", "micros", "microsecond", "microseconds" -> ChronoUnit.MICROS;
                    case "milli", "millis", "millisecond", "milliseconds" -> ChronoUnit.MILLIS;
                    case "second", "seconds" -> ChronoUnit.SECONDS;
                    case "minute", "minutes" -> ChronoUnit.MINUTES;
                    case "hour", "hours" -> ChronoUnit.HOURS;
                    case "day", "days" -> ChronoUnit.DAYS;
                    case "week", "weeks" -> ChronoUnit.WEEKS;
                    case "month", "months" -> ChronoUnit.MONTHS;
                    case "year", "years" -> ChronoUnit.YEARS;
                    case "decade", "decades" -> ChronoUnit.DECADES;
                    case "century", "centuries" -> ChronoUnit.CENTURIES;
                    case "millennium", "millennia" -> ChronoUnit.MILLENNIA;
                    default -> null;
                };
                if (unit == null) return null;
                temporalAmount = temporalAmount.plus(amount, unit);
                String remaining = matcher.group(4);
                if (remaining.isBlank()) return temporalAmount.normalize();
                matcher = PERIOD_PATTERN_CONT.matcher(remaining);
            }
        }
        return null;
    }

    private static @Nullable Period castToPeriod(@Nullable Object object) {
        TemporalAmount temporalAmount = castToTemporalAmount(object);
        if (temporalAmount == null) return null;
        if (temporalAmount instanceof Duration duration && duration.isZero()) return Period.ZERO;
        return temporalAmount instanceof Period period ? period : null;
    }

    private static @Nullable Duration castToDuration(@Nullable Object object) {
        TemporalAmount temporalAmount = castToTemporalAmount(object);
        if (temporalAmount == null) return null;
        if (temporalAmount instanceof Period period && period.isZero()) return Duration.ZERO;
        return temporalAmount instanceof Duration duration ? duration : null;
    }

    private static @Nullable DayOfWeek castToDayOfWeek(@Nullable Object object) {
        if (object instanceof String string) {
            return switch (string.toLowerCase(Locale.ROOT)) {
                case "mon", "monday" -> DayOfWeek.MONDAY;
                case "tue", "tuesday" -> DayOfWeek.TUESDAY;
                case "wed", "wednesday" -> DayOfWeek.WEDNESDAY;
                case "thu", "thursday" -> DayOfWeek.THURSDAY;
                case "fri", "friday" -> DayOfWeek.FRIDAY;
                case "sat", "saturday" -> DayOfWeek.SATURDAY;
                case "sun", "sunday" -> DayOfWeek.SUNDAY;
                default -> null;
            };
        }
        return null;
    }

    private static @Nullable Month castToMonth(@Nullable Object object) {
        if (object instanceof String string) {
            return switch (string.toLowerCase(Locale.ROOT)) {
                case "jan", "january" -> Month.JANUARY;
                case "feb", "february" -> Month.FEBRUARY;
                case "mar", "march" -> Month.MARCH;
                case "apr", "april" -> Month.APRIL;
                case "may" -> Month.MAY;
                case "jun", "june" -> Month.JUNE;
                case "jul", "july" -> Month.JULY;
                case "aug", "august" -> Month.AUGUST;
                case "sep", "september" -> Month.SEPTEMBER;
                case "oct", "october" -> Month.OCTOBER;
                case "nov", "november" -> Month.NOVEMBER;
                case "dec", "december" -> Month.DECEMBER;
                default -> null;
            };
        }
        if (object instanceof Integer num && num >= 1 && num <= 12) {
            return Month.of(num);
        }
        return null;
    }

    private static @Nullable Path castToPath(@Nullable Object object) {
        if (object instanceof File file) {
            return file.toPath();
        }
        if (object instanceof String str) {
            return CrocusRuntime.get().path().resolve(str);
        }
        return null;
    }

    private static @Nullable File castToFile(@Nullable Object object) {
        Path path = castToPath(object);
        if (path != null && Objects.equals(path.getFileSystem(), FileSystems.getDefault())) {
            return path.toFile();
        }
        return null;
    }

    private static @Nullable URI castToURI(@Nullable Object object) {
        if (object instanceof URL url) {
            try {
                return url.toURI();
            } catch (URISyntaxException e) {
                //
            }
        }
        if (object instanceof Path path) {
            return path.toUri();
        }
        if (object instanceof File file) {
            return file.toURI();
        }
        if (object instanceof String str) {
            try {
                URI uri = new URI(str);
                if (!uri.isAbsolute() && uri.getScheme() == null) {
                    // Relative path
                    return CrocusRuntime.get().path().toUri().resolve(uri);
                } else {
                    return uri;
                }
            } catch (URISyntaxException e) {
                //
            }
        }
        return null;
    }

    private static @Nullable URL castToURL(@Nullable Object object) {
        if (object instanceof String str) {
            try {
                return new URI(str).toURL();
            } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
                //
            }
        }
        URI uri = castToURI(object);
        if (uri != null) {
            try {
                return uri.toURL();
            } catch (MalformedURLException | IllegalArgumentException e) {
                //
            }
        }
        return null;
    }

    private static @Nullable Resource castToResource(@Nullable Object object) {
        if (object instanceof File file) {
            return new PathResource(file.toPath());
        } else if (object instanceof Path path) {
            return new PathResource(path);
        } else {
            URI uri = castToURI(object);
            if (uri != null && "data".equalsIgnoreCase(uri.getScheme())) {
                return MemoryResource.of(uri);
            } else {
                URL url = castToURL(object);
                if (url != null) return UrlResource.of(url);
            }
        }
        if (object instanceof byte[] data) {
            return new MemoryResource(data);
        }
        return null;
    }

    private static @Nullable Charset castToCharset(@Nullable Object object) {
        if (object instanceof String str && Charset.isSupported(str)) {
            return Charset.forName(str);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enum<T>> @Nullable Enum<?> castToEnum(@Nullable Object object, Class<?> enumCls) {
        return typesafeCastToEnum(object, (Class<T>) enumCls);
    }

    private static <T extends Enum<T>> @Nullable T typesafeCastToEnum(@Nullable Object object, Class<T> enumCls) {
        if (object instanceof String string) {
            T[] enumConstants = enumCls.getEnumConstants();
            if (!enumCls.isEnum() || enumConstants == null) return null;
            for (T constant : enumConstants) {
                if (Objects.equals(constant.name(), string)) {
                    return constant;
                }
            }
            String lowercase = string.toLowerCase(Locale.ROOT);
            Set<T> caseInsensitiveMatches = Arrays.stream(enumConstants)
                    .filter(constant -> Objects.equals(constant.name().toLowerCase(Locale.ROOT), lowercase))
                    .collect(Collectors.toUnmodifiableSet());
            if (caseInsensitiveMatches.size() == 1) return caseInsensitiveMatches.iterator().next();
        }
        return null;
    }
}
