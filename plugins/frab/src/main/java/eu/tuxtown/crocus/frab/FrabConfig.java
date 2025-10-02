package eu.tuxtown.crocus.frab;

import eu.tuxtown.crocus.api.resource.Resource;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.NoSuchElementException;

@NotNullByDefault
public class FrabConfig {

    @Nullable private Resource source;
    private Charset charset;
    private ZoneId timezone; // fallback timezone

    public FrabConfig() {
        this.charset = StandardCharsets.UTF_8;
        this.timezone = ZoneId.systemDefault();
    }

    public void source(Resource source) {
        this.source = source;
    }

    public void charset(Charset charset) {
        this.charset = charset;
    }

    public void timezone(ZoneId timezone) {
        this.timezone = timezone;
    }

    public Resource getSource() {
        if (this.source == null) throw new NoSuchElementException("No frab source set.");
        return this.source;
    }

    public Charset getCharset() {
        return this.charset;
    }

    public ZoneId getTimezone() {
        return this.timezone;
    }
}
