package eu.tuxtown.crocus.impl.resource;

import eu.tuxtown.crocus.api.resource.Resource;
import eu.tuxtown.crocus.core.CrocusRuntime;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@NotNullByDefault
public record PathResource(Path path) implements Resource {

    @Override
    public URI describe() {
        return this.path.toUri();
    }

    @Override
    public InputStream openStream() throws IOException {
        return Files.newInputStream(this.resolvePath());
    }

    @Override
    public Reader openReader(Charset charset) throws IOException {
        return Files.newBufferedReader(this.resolvePath(), charset);
    }

    @Override
    public Optional<String> probeContentType() throws IOException {
        return Optional.ofNullable(Files.probeContentType(this.resolvePath()));
    }

    @Override
    public String toString() {
        return this.path.toString();
    }

    private Path resolvePath() {
        if (this.path().getFileSystem() == FileSystems.getDefault() && !this.path().isAbsolute()) {
            return CrocusRuntime.get().path().resolve(this.path());
        } else {
            return this.path();
        }
    }
}
