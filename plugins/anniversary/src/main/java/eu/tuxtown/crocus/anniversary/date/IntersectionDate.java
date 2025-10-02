package eu.tuxtown.crocus.anniversary.date;

import org.jetbrains.annotations.NotNullByDefault;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NotNullByDefault
public class IntersectionDate implements AnniversaryDate {

    private final List<AnniversaryDate> dates;

    public IntersectionDate(List<AnniversaryDate> dates) {
        this.dates = dates.stream()
                .flatMap(date -> date instanceof IntersectionDate intersection ? intersection.dates.stream() : Stream.of(date))
                .filter(date -> date != AlwaysDate.INSTANCE)
                .toList();
    }

    @Override
    public Stream<LocalDate> resolve(LocalDate from, LocalDate to) {
        if (this.dates.isEmpty()) return AlwaysDate.INSTANCE.resolve(from, to);
        if (this.dates.size() == 1) return this.dates.getFirst().resolve(from, to);
        List<Set<LocalDate>> resolved = this.dates.stream()
                .map(date -> date.resolve(from, to).collect(Collectors.toUnmodifiableSet()))
                .toList();
        return resolved.getFirst().stream()
                .filter(date -> resolved.stream().allMatch(set -> set.contains(date)));
    }
}
