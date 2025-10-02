package eu.tuxtown.crocus.anniversary;

import eu.tuxtown.crocus.anniversary.date.AlwaysDate;
import eu.tuxtown.crocus.anniversary.date.AnniversaryDate;
import eu.tuxtown.crocus.anniversary.date.FilterDate;
import eu.tuxtown.crocus.anniversary.date.SimpleDate;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@NotNullByDefault
public class AnniversaryConfig {

    private final List<Anniversary> anniversaries = new ArrayList<>();

    @Nullable private LocalDate since;
    private LocalDate until = LocalDate.now().plusYears(2).with(TemporalAdjusters.firstDayOfYear()).minusDays(1);

    public void since(LocalDate since) {
        this.since = since;
    }

    public void until(LocalDate until) {
        this.until = until;
    }

    public void day(String name, AnniversaryDate.Wrapper date) {
        this.day(name, "", date);
    }

    public void day(String name, Supplier<AnniversaryDate.Wrapper> date) {
        this.day(name, "", date);
    }

    public void day(String name, String description, AnniversaryDate.Wrapper date) {
        this.day(name, ignored -> description, date);
    }

    public void day(String name, String description, Supplier<AnniversaryDate.Wrapper> date) {
        this.day(name, ignored -> description, date);
    }

    public void day(String name, Function<LocalDate, String> description, AnniversaryDate.Wrapper date) {
        this.day(name, description, () -> date);
    }

    public void day(String name, Function<LocalDate, String> description, Supplier<AnniversaryDate.Wrapper> date) {
        Anniversary anniversary = new Anniversary(name, description, AnniversaryDate.unwrap(date.get()));
        this.anniversaries.add(anniversary);
    }

    public LocalDate getSince() {
        if (this.since == null) throw new NoSuchElementException("No start date set.");
        return this.since;
    }

    public LocalDate getUntil() {
        return this.until;
    }

    public List<Anniversary> getAnniversaries() {
        return List.copyOf(this.anniversaries);
    }

    public AnniversaryDate.Wrapper always() {
        return AlwaysDate.INSTANCE.wrap();
    }

    public AnniversaryDate.Wrapper on(String filter) {
        return FilterDate.parseFilter(AlwaysDate.INSTANCE, filter).wrap();
    }

    public AnniversaryDate.Wrapper on(int dayOfMonth) {
        return FilterDate.parseFilter(AlwaysDate.INSTANCE, dayOfMonth).wrap();
    }

    public AnniversaryDate.Wrapper on(DayOfWeek dayOfWeek) {
        return FilterDate.parseFilter(AlwaysDate.INSTANCE, dayOfWeek).wrap();
    }

    public AnniversaryDate.Wrapper on(Month month) {
        return FilterDate.parseFilter(AlwaysDate.INSTANCE, month).wrap();
    }

    public AnniversaryDate.Wrapper on(int dayOfMonth, Month month) {
        return new SimpleDate(month, dayOfMonth).wrap();
    }

    public AnniversaryDate.Wrapper in(String filter) {
        return FilterDate.parseFilter(AlwaysDate.INSTANCE, filter).wrap();
    }

    public AnniversaryDate.Wrapper in(int dayOfMonth) {
        return FilterDate.parseFilter(AlwaysDate.INSTANCE, dayOfMonth).wrap();
    }

    public AnniversaryDate.Wrapper in(DayOfWeek dayOfWeek) {
        return FilterDate.parseFilter(AlwaysDate.INSTANCE, dayOfWeek).wrap();
    }

    public AnniversaryDate.Wrapper in(Month month) {
        return FilterDate.parseFilter(AlwaysDate.INSTANCE, month).wrap();
    }

    public AnniversaryDate.Wrapper in(int dayOfMonth, Month month) {
        return new SimpleDate(month, dayOfMonth).wrap();
    }

    public AnniversaryDate.Wrapper only(Predicate<LocalDate> filter) {
        return new FilterDate(AlwaysDate.INSTANCE, filter).wrap();
    }
}
