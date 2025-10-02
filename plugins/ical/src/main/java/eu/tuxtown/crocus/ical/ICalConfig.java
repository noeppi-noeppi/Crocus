package eu.tuxtown.crocus.ical;

import eu.tuxtown.crocus.api.resource.Resource;
import eu.tuxtown.crocus.sesquiannual.SequenceBehavior;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.util.NoSuchElementException;

@NotNullByDefault
public class ICalConfig {

    @Nullable private Resource source;
    private Charset charset;
    private ZoneId timezone; // fallback timezone
    private TemporalAmount repeatFor; // From event start
    private TemporalAmount repeatFromNow; // From current day
    private SequenceBehavior sequences; // From current day

    public ICalConfig() {
        this.charset = StandardCharsets.UTF_8;
        this.timezone = ZoneId.systemDefault();
        this.repeatFor = Period.of(1, 0, 0);
        this.repeatFromNow = Period.of(1, 0, 0);
        this.sequences = SequenceBehavior.UNIFORM;
    }

    public void source(Resource source) {
        this.source = source;
    }

    public void timezone(ZoneId timezone) {
        this.timezone = timezone;
    }

    public void charset(Charset charset) {
        this.charset = charset;
    }

    public void repeatFor(TemporalAmount repeatFor) {
        this.repeatFor = repeatFor;
    }

    public void repeatFromNow(TemporalAmount repeatFromNow) {
        this.repeatFromNow = repeatFromNow;
    }

    public void sequences(SequenceBehavior sequences) {
        this.sequences = sequences;
    }

    public Resource getSource() {
        if (this.source == null) throw new NoSuchElementException("No iCal source set.");
        return this.source;
    }

    public Charset getCharset() {
        return this.charset;
    }

    public ZoneId getTimezone() {
        return this.timezone;
    }

    public TemporalAmount getRepeatFor() {
        return this.repeatFor;
    }

    public TemporalAmount getRepeatFromNow() {
        return this.repeatFromNow;
    }

    public SequenceBehavior getSequences() {
        return this.sequences;
    }
}
