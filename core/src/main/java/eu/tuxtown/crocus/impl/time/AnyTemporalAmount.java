package eu.tuxtown.crocus.impl.time;

import org.jetbrains.annotations.NotNullByDefault;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@NotNullByDefault
public class AnyTemporalAmount implements TemporalAmount {

    public static final AnyTemporalAmount ZERO = new AnyTemporalAmount(true, Map.of());

    private boolean isNormalized;
    private final Map<TemporalUnit, Long> values;

    private AnyTemporalAmount(boolean isNormalized, Map<TemporalUnit, Long> values) {
        this.isNormalized = isNormalized;
        this.values = values;
    }

    @Override
    public long get(TemporalUnit unit) {
        return this.values.getOrDefault(unit, (long) 0);
    }

    @Override
    public List<TemporalUnit> getUnits() {
        return this.values.keySet().stream().toList();
    }

    @Override
    public Temporal addTo(Temporal temporal) {
        for (TemporalUnit unit : this.values.keySet()) {
            temporal = temporal.plus(this.values.get(unit), unit);
        }
        return temporal;
    }

    @Override
    public Temporal subtractFrom(Temporal temporal) {
        for (TemporalUnit unit : this.values.keySet()) {
            temporal = temporal.minus(this.values.get(unit), unit);
        }
        return temporal;
    }

    public AnyTemporalAmount minus(TemporalAmount amount) {
        AnyTemporalAmount ret = this;
        for (TemporalUnit unit : amount.getUnits()) ret = ret.minus(amount.get(unit), unit);
        return ret;
    }

    public AnyTemporalAmount plus(TemporalAmount amount) {
        AnyTemporalAmount ret = this;
        for (TemporalUnit unit : amount.getUnits()) ret = ret.plus(amount.get(unit), unit);
        return ret;
    }

    public AnyTemporalAmount minus(long value, TemporalUnit unit) {
        return this.plus(-value, unit);
    }

    public AnyTemporalAmount plus(long value, TemporalUnit unit) {
        if (unit == ChronoUnit.FOREVER) throw new UnsupportedOperationException("Can't add forever to this amount.");
        if (unit == ChronoUnit.ERAS) throw new UnsupportedOperationException("Can't add eras to this amount.");
        if (value == 0) return this;
        long newValue = this.values.getOrDefault(unit, (long) 0) + value;
        Map<TemporalUnit, Long> map = new HashMap<>(this.values);
        if (newValue == 0) map.remove(unit);
        else map.put(unit, newValue);
        return new AnyTemporalAmount(false, map);
    }

    public TemporalAmount normalize() {
        if (this.isNormalized) return this;
        // Normalization reduces all chrono units down to nanos, seconds, days, months and years
        // and puts them into their usual range.

        Map<TemporalUnit, Long> map = new HashMap<>();
        long nanos = 0;
        long seconds = 0;
        long days = 0;
        long months = 0;
        long years = 0;

        for (TemporalUnit unit : this.values.keySet()) {
            long value = this.values.get(unit);
            switch ((ChronoUnit) unit) {
                case NANOS -> nanos += value;
                case MICROS -> nanos += value * 1000;
                case MILLIS -> nanos += value * 1000000;
                case SECONDS -> seconds += value;
                case MINUTES -> seconds += value * 60;
                case HOURS -> seconds += value * 3600;
                case HALF_DAYS -> seconds += value * 43200;
                case DAYS -> days += value;
                case WEEKS -> days += value * 7;
                case MONTHS -> months += value;
                case YEARS -> years += value;
                case DECADES -> years += value * 10;
                case CENTURIES -> years += value * 100;
                case MILLENNIA -> years += value * 1000;
                default -> map.put(unit, value); // Non-chrono unit.
            }
            seconds += nanos / 1000000000;
            nanos = nanos % 1000000000;
            days += seconds / 86400;
            seconds = seconds % 86400;

            years += months / 12;
            months = months % 12;
        }

        if (nanos != 0) map.put(ChronoUnit.NANOS, nanos);
        if (seconds != 0) map.put(ChronoUnit.SECONDS, seconds);
        if (days != 0) map.put(ChronoUnit.DAYS, days);
        if (months != 0) map.put(ChronoUnit.MONTHS, months);
        if (years != 0) map.put(ChronoUnit.YEARS, years);

        // Try to convert to standard type of temporal amount.
        if (map.isEmpty()) return Duration.ZERO;
        if (map.keySet().stream().allMatch(unit -> unit == ChronoUnit.DAYS || unit == ChronoUnit.MONTHS || unit == ChronoUnit.YEARS)) {
            return Period.of(
                    (int) (long) map.getOrDefault(ChronoUnit.YEARS, (long) 0),
                    (int) (long) map.getOrDefault(ChronoUnit.MONTHS, (long) 0),
                    (int) (long) map.getOrDefault(ChronoUnit.DAYS, (long) 0)
            );
        }
        if (map.keySet().stream().allMatch(unit -> unit == ChronoUnit.NANOS || unit == ChronoUnit.SECONDS || unit == ChronoUnit.DAYS)) {
            return Duration.ofSeconds(
                    (86400 * map.getOrDefault(ChronoUnit.DAYS, (long) 0)) + map.getOrDefault(ChronoUnit.SECONDS, (long) 0),
                    map.getOrDefault(ChronoUnit.NANOS, (long) 0)
            );
        }

        // If we are no standard temporal amount and the map is the same as before, we are already normalized but the flag was not set.
        if (Objects.equals(this.values, map)) {
            this.isNormalized = true;
            return this;
        } else {
            return new AnyTemporalAmount(true, map);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        AnyTemporalAmount that = (AnyTemporalAmount) o;
        return Objects.equals(this.values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.values);
    }

    @Override
    public String toString() {
        return this.values.entrySet().stream().map(entry -> entry.getValue() + " " + entry.getKey()).collect(Collectors.joining(" + "));
    }
}
