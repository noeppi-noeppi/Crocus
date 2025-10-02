package eu.tuxtown.crocus.sesquiannual;

import org.jetbrains.annotations.NotNullByDefault;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * An {@link VEventTimespan event timespan} contains the start date of an event and if available also its end date.
 * It is always either an instance of {@link VEventTimespan.Date} for all-day events or {@link VEventTimespan.Time}
 * for timed events.
 */
@NotNullByDefault
public sealed interface VEventTimespan permits VEventTimespan.Date, VEventTimespan.Time {

    /**
     * An {@link VEventTimespan event timespan} for all-day events.
     *
     * @param start The first day of the event.
     * @param end   The last day of the event. <b>This day is still part of the event, the event ends on 0:00 the next day.</b>
     */
    @NotNullByDefault
    record Date(LocalDate start, Optional<LocalDate> end) implements VEventTimespan {

        @Override
        public String toString() {
            if (this.end().isPresent()) {
                return "VEventTimespan.Date(" + this.start() + " - " + this.end().get() + ")";
            } else {
                return "VEventTimespan.Date(" + this.start() + " - <>)";
            }
        }
    }

    /**
     * An {@link VEventTimespan event timespan} for timed events.
     *
     * @param start The timestamp on which the event starts.
     * @param end   The timestamp on which the event ends.
     */
    @NotNullByDefault
    record Time(Instant start, Optional<Instant> end) implements VEventTimespan {

        @Override
        public String toString() {
            if (this.end().isPresent()) {
                return "VEventTimespan.Time(" + this.start() + " - " + this.end().get() + ")";
            } else {
                return "VEventTimespan.Time(" + this.start() + " - <>)";
            }
        }
    }
}
