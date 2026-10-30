package eu.tuxtown.crocus.csv;

import eu.tuxtown.crocus.api.delegate.DelegateConfiguration;
import eu.tuxtown.crocus.api.resource.Resource;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.OptionalInt;

@NotNullByDefault
public class CsvConfig {

    private @Nullable Resource source;
    private Charset charset;
    private String separator;
    private OptionalInt comment;
    private @Nullable DelegateConfiguration factory;

    public CsvConfig() {
        this.charset = StandardCharsets.UTF_8;
        this.separator = ",";
        this.comment = OptionalInt.empty();
    }

    public void source(Resource source) {
        this.source = source;
    }

    public void charset(Charset charset) {
        this.charset = charset;
    }

    public void separator(String separator) {
        this.separator = separator;
    }

    public void comment(char comment) {
        this.comment = OptionalInt.of(comment);
    }

    public void events(DelegateConfiguration factory) {
        this.factory = factory;
    }

    public Resource getSource() {
        if (this.source == null) throw new NoSuchElementException("No csv source set.");
        return this.source;
    }

    public Charset getCharset() {
        return this.charset;
    }

    public String getSeparator() {
        return this.separator;
    }

    public OptionalInt getComment() {
        return this.comment;
    }

    public DelegateConfiguration getFactory() {
        if (this.factory == null) throw new NoSuchElementException("No csv source set.");
        return this.factory;
    }
}
