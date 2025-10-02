package eu.tuxtown.crocus.anniversary.date;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.Month;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@NotNullByDefault
public record SimpleDate(Month month, int day) implements AnniversaryDate {

    @Override
    public Stream<LocalDate> resolve(LocalDate from, LocalDate to) {
        return IntStream.rangeClosed(from.getYear(), to.getYear())
                .mapToObj(year -> this.newDate(year, this.month(), this.day()))
                .filter(Objects::nonNull)
                .filter(date -> !from.isAfter(date) && !to.isBefore(date));
    }

    private @Nullable LocalDate newDate(int year, Month month, int day) {
        LocalDate date = LocalDate.of(year, month, 1).plusDays(day - 1);
        if (date.getYear() == year && date.getMonth() == month) return date;
        return null; // Out of range
    }
}
