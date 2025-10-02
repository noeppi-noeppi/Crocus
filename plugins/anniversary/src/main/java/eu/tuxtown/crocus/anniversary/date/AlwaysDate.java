package eu.tuxtown.crocus.anniversary.date;

import org.jetbrains.annotations.NotNullByDefault;

import java.time.LocalDate;
import java.util.stream.Stream;

@NotNullByDefault
public enum AlwaysDate implements AnniversaryDate {
    INSTANCE;

    @Override
    public Stream<LocalDate> resolve(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) return Stream.empty();
        return Stream.iterate(from, date -> !date.isAfter(to), date -> date.plusDays(1));
    }
}
