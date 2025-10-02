package eu.tuxtown.crocus.anniversary.date;

import org.jetbrains.annotations.NotNullByDefault;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAmount;
import java.util.stream.Stream;

@NotNullByDefault
public class AdjustedDate implements AnniversaryDate {

    private final AnniversaryDate date;
    private final TemporalAdjuster adjuster;
    private final TemporalAmount maximumOffset;

    public AdjustedDate(AnniversaryDate date, TemporalAdjuster adjuster, TemporalAmount maximumOffset) {
        this.date = date;
        this.adjuster = adjuster;
        this.maximumOffset = maximumOffset;
    }

    @Override
    public Stream<LocalDate> resolve(LocalDate from, LocalDate to) {
        return this.date.resolve(from.minus(this.maximumOffset), to.plus(this.maximumOffset))
                .map(date -> date.with(this.adjuster))
                .filter(date -> !from.isAfter(date) && !to.isBefore(date))
                .distinct();
    }
}
