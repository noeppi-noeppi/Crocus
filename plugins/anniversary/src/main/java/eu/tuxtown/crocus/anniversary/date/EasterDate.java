package eu.tuxtown.crocus.anniversary.date;

import org.jetbrains.annotations.NotNullByDefault;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@NotNullByDefault
public enum EasterDate implements AnniversaryDate {
    INSTANCE;

    @Override
    public Stream<LocalDate> resolve(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) return Stream.of();
        return IntStream.rangeClosed(Math.max(from.getYear(), 1583), to.getYear())
                .mapToObj(this::easterDate)
                .filter(date -> !from.isAfter(date) && !to.isBefore(date));
    }

    private LocalDate easterDate(int year) {
        if (year <= 0) throw new IllegalArgumentException();
        // Using the Gauß easter algorithm
        int a = year % 19;
        int k = year / 100;
        int p = (8 * k + 13) / 25;
        int q = k / 4;
        int M = (((15 + k - p - q) % 30) + 30) % 30;
        int d = (((19 * a + M) % 30) + 30) % 30;
        if (d == 29 || (d == 28 && ((((11 * M + 11) % 30) + 30) % 30) < 19)) {
            d -= 1;
        }
        return LocalDate.of(year, Month.MARCH, 22).plusDays(d).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }
}
