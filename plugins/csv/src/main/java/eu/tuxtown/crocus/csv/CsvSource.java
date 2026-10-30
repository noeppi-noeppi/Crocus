package eu.tuxtown.crocus.csv;

import de.siegmar.fastcsv.reader.CommentStrategy;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;
import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.api.calendar.EventSource;
import eu.tuxtown.crocus.api.delegate.DelegateConfigurable;
import eu.tuxtown.crocus.api.delegate.DelegateConfiguration;
import eu.tuxtown.crocus.api.resource.Resource;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

@NotNullByDefault
public class CsvSource implements EventSource {

    private final Resource res;
    private final Charset charset;
    private final String separator;
    private final OptionalInt comment;
    private final DelegateConfiguration factory;

    public CsvSource(CsvConfig cfg) {
        this.res = cfg.getSource();
        this.charset = cfg.getCharset();
        this.separator = cfg.getSeparator();
        this.comment = cfg.getComment();
        this.factory = cfg.getFactory();
    }

    @Override
    public String name() {
        return "csv:" + this.res;
    }

    @Override
    public List<Event> retrieveEvents() throws IOException {
        CsvReader.CsvReaderBuilder csvBuilder = CsvReader.builder();
        csvBuilder.fieldSeparator(this.separator);
        if (this.comment.isPresent() && this.comment.getAsInt() >= 0 && this.comment.getAsInt() <= Character.MAX_VALUE) {
            csvBuilder.commentStrategy(CommentStrategy.SKIP);
            csvBuilder.commentCharacter((char) this.comment.getAsInt());
        }

        List<Event> events = new ArrayList<>();
        int idx = 0;
        try (Reader reader = this.res.openReader(this.charset); CsvReader<NamedCsvRecord> csv = csvBuilder.ofNamedCsvRecord(reader)) {
            for (NamedCsvRecord record : csv) {
                EventConfigurable configurable = new EventConfigurable("#" + (idx++));
                Object dsl = Crocus.makeDynamicObject(record.getFieldsAsMap());
                events.add(this.factory.configure(configurable, dsl));
            }
        }
        return Collections.unmodifiableList(events);
    }

    private record EventConfigurable(String eventId) implements DelegateConfigurable<Event, Event.Builder> {

        @Override
        public Event.Builder createDelegate() {
            return Event.builder(this.eventId());
        }

        @Override
        public Event create(Event.Builder delegate) {
            try {
                return delegate.build();
            } catch (Exception e) {
                throw new RuntimeException("Failed to assemble event from CSV file.");
            }
        }
    }
}
