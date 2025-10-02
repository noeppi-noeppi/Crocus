package eu.tuxtown.crocus.anniversary.date;

import org.jetbrains.annotations.NotNullByDefault;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

@NotNullByDefault
public class UnionDate implements AnniversaryDate {

    private final List<AnniversaryDate> dates;

    public UnionDate(List<AnniversaryDate> dates) {
        this.dates = dates.stream()
                .flatMap(date -> date instanceof UnionDate union ? union.dates.stream() : Stream.of(date))
                .toList();
    }

    @Override
    public Stream<LocalDate> resolve(LocalDate from, LocalDate to) {
        return this.dates.stream().flatMap(date -> date.resolve(from, to)).distinct();
    }
}
