package eu.tuxtown.crocus.anniversary.date;

import org.jetbrains.annotations.NotNullByDefault;

import java.time.LocalDate;
import java.time.temporal.TemporalAmount;
import java.util.stream.Stream;

@NotNullByDefault
public class OffsetDate implements AnniversaryDate {

    private final AnniversaryDate date;
    private final TemporalAmount offset;
    private final boolean subtract;

    public OffsetDate(AnniversaryDate date, TemporalAmount offset, boolean subtract) {
        this.date = date;
        this.offset = offset;
        this.subtract = subtract;
    }

    @Override
    public Stream<LocalDate> resolve(LocalDate from, LocalDate to) {
        if (this.subtract) {
            return this.date.resolve(from.plus(this.offset), to.plus(this.offset)).map(date -> date.minus(this.offset));
        } else {
            return this.date.resolve(from.minus(this.offset), to.minus(this.offset)).map(date -> date.plus(this.offset));
        }
    }
}
