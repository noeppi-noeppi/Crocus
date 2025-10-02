package eu.tuxtown.crocus.frab;

import org.jetbrains.annotations.NotNullByDefault;

import java.time.temporal.*;

@NotNullByDefault
public class Util {

    public static final TemporalField EPOCH_HOUR = new TemporalField() {

        @Override
        public TemporalUnit getBaseUnit() {
            return ChronoUnit.HOURS;
        }

        @Override
        public TemporalUnit getRangeUnit() {
            return ChronoUnit.FOREVER;
        }

        @Override
        public ValueRange range() {
            return ValueRange.of(Long.MIN_VALUE, Long.MAX_VALUE);
        }

        @Override
        public boolean isDateBased() {
            return false;
        }

        @Override
        public boolean isTimeBased() {
            return false;
        }

        @Override
        public boolean isSupportedBy(TemporalAccessor temporal) {
            return temporal.isSupported(ChronoField.INSTANT_SECONDS);
        }

        @Override
        public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
            return this.range();
        }

        @Override
        public long getFrom(TemporalAccessor temporal) {
            long epochSeconds = temporal.getLong(ChronoField.INSTANT_SECONDS);
            return epochSeconds / 3600;
        }

        @Override
        public <R extends Temporal> R adjustInto(R temporal, long newValue) {
            long epochDay = newValue / 24;
            long hour = newValue - 24 * epochDay;
            if (hour < 0) {
                hour += 24;
                epochDay -= 1;
            }
            return ChronoField.HOUR_OF_DAY.adjustInto(ChronoField.EPOCH_DAY.adjustInto(temporal, epochDay), hour);
        }
    };
}
