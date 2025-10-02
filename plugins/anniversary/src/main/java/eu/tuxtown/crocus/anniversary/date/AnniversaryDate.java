package eu.tuxtown.crocus.anniversary.date;

import eu.tuxtown.crocus.api.Crocus;
import org.jetbrains.annotations.NotNullByDefault;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

@NotNullByDefault
public interface AnniversaryDate {

    Stream<LocalDate> resolve(LocalDate from, LocalDate to);

    default Wrapper wrap() {
        return new Wrapper(this);
    }

    static AnniversaryDate unwrap(Wrapper wrapper) {
        return wrapper.date;
    }

    @NotNullByDefault
    class Wrapper {

        private final AnniversaryDate date;

        private Wrapper(AnniversaryDate date) {
            this.date = date;
            Crocus.makeDslObject(this);
        }

        public Wrapper and(Wrapper other) {
            return new IntersectionDate(List.of(this.date, other.date)).wrap();
        }

        public Wrapper or(Wrapper other) {
            return new UnionDate(List.of(this.date, other.date)).wrap();
        }

        public Wrapper plus(TemporalAmount amount) {
            return new OffsetDate(this.date, amount, false).wrap();
        }

        public Wrapper minus(TemporalAmount amount) {
            return new OffsetDate(this.date, amount, true).wrap();
        }

        public Wrapper first(DayOfWeek dayOfWeek) {
            return new AdjustedDate(this.date, TemporalAdjusters.nextOrSame(dayOfWeek), Period.of(0, 0, 7)).wrap();
        }

        public Wrapper last(DayOfWeek dayOfWeek) {
            return new AdjustedDate(this.date, TemporalAdjusters.previousOrSame(dayOfWeek), Period.of(0, 0, 7)).wrap();
        }

        public Wrapper next(DayOfWeek dayOfWeek) {
            return new AdjustedDate(this.date, TemporalAdjusters.next(dayOfWeek), Period.of(0, 0, 8)).wrap();
        }

        public Wrapper previous(DayOfWeek dayOfWeek) {
            return new AdjustedDate(this.date, TemporalAdjusters.previous(dayOfWeek), Period.of(0, 0, 8)).wrap();
        }

        public Wrapper firstInMonth(DayOfWeek dayOfWeek) {
            return new AdjustedDate(this.date, TemporalAdjusters.firstInMonth(dayOfWeek), Period.of(0, 1, 8)).wrap();
        }

        public Wrapper lastInMonth(DayOfWeek dayOfWeek) {
            return new AdjustedDate(this.date, TemporalAdjusters.lastInMonth(dayOfWeek), Period.of(0, 1, 8)).wrap();
        }

        public Wrapper firstDayOfMonth() {
            return new AdjustedDate(this.date, TemporalAdjusters.firstDayOfMonth(), Period.of(0, 1, 1)).wrap();
        }

        public Wrapper lastDayOfMonth() {
            return new AdjustedDate(this.date, TemporalAdjusters.lastDayOfMonth(), Period.of(0, 1, 1)).wrap();
        }

        public Wrapper firstDayOfNextMonth() {
            return new AdjustedDate(this.date, TemporalAdjusters.firstDayOfNextMonth(), Period.of(0, 1, 2)).wrap();
        }

        public Wrapper firstDayOfYear() {
            return new AdjustedDate(this.date, TemporalAdjusters.firstDayOfYear(), Period.of(1, 0, 1)).wrap();
        }

        public Wrapper lastDayOfYear() {
            return new AdjustedDate(this.date, TemporalAdjusters.lastDayOfYear(), Period.of(1, 0, 1)).wrap();
        }

        public Wrapper firstDayOfNextYear() {
            return new AdjustedDate(this.date, TemporalAdjusters.firstDayOfNextYear(), Period.of(1, 0, 2)).wrap();
        }

        public Wrapper on(String filter) {
            return FilterDate.parseFilter(this.date, filter).wrap();
        }

        public Wrapper on(int dayOfMonth) {
            return FilterDate.parseFilter(this.date, dayOfMonth).wrap();
        }

        public Wrapper on(DayOfWeek dayOfWeek) {
            return FilterDate.parseFilter(this.date, dayOfWeek).wrap();
        }

        public Wrapper on(Month month) {
            return FilterDate.parseFilter(this.date, month).wrap();
        }

        public Wrapper on(int dayOfMonth, Month month) {
            return new SimpleDate(month, dayOfMonth).wrap();
        }

        public Wrapper in(String filter) {
            return FilterDate.parseFilter(this.date, filter).wrap();
        }

        public Wrapper in(int dayOfMonth) {
            return FilterDate.parseFilter(this.date, dayOfMonth).wrap();
        }

        public Wrapper in(DayOfWeek dayOfWeek) {
            return FilterDate.parseFilter(this.date, dayOfWeek).wrap();
        }

        public Wrapper in(Month month) {
            return FilterDate.parseFilter(this.date, month).wrap();
        }

        public Wrapper in(int dayOfMonth, Month month) {
            return new SimpleDate(month, dayOfMonth).wrap();
        }

        public Wrapper only(Predicate<LocalDate> filter) {
            return new FilterDate(this.date, filter).wrap();
        }
    }
}
