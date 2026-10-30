package eu.tuxtown.crocus.csv;

import eu.tuxtown.crocus.api.service.EventSourceType;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public class CsvSourceType implements EventSourceType<CsvSource, CsvConfig> {

    @Override
    public String name() {
        return "csv";
    }

    @Override
    public CsvConfig createDelegate() {
        return new CsvConfig();
    }

    @Override
    public CsvSource create(CsvConfig delegate) {
        return new CsvSource(delegate);
    }
}
