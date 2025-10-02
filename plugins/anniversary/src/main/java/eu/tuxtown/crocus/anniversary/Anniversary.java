package eu.tuxtown.crocus.anniversary;

import eu.tuxtown.crocus.anniversary.date.AnniversaryDate;
import org.jetbrains.annotations.NotNullByDefault;

import java.time.LocalDate;
import java.util.function.Function;

@NotNullByDefault
public record Anniversary(String name, Function<LocalDate, String> description, AnniversaryDate date) {}
