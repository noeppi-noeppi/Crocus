package eu.tuxtown.crocus.builtin;

import eu.tuxtown.crocus.api.resource.Resource;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

@NotNullByDefault
public class DumpConfig {

    @Nullable private Resource source;
    private Charset charset;

    public DumpConfig() {
        this.charset = StandardCharsets.UTF_8;
    }

    public void source(Resource source) {
        this.source = source;
    }

    public void charset(Charset charset) {
        this.charset = charset;
    }

    public Resource getSource() {
        if (this.source == null) throw new NoSuchElementException("No dump source set.");
        return this.source;
    }

    public Charset getCharset() {
        return this.charset;
    }
}
