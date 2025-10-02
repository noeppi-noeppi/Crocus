package eu.tuxtown.crocus.api.resource;

import eu.tuxtown.crocus.impl.resource.PathResource;
import eu.tuxtown.crocus.impl.resource.UrlResource;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A generic resource where data can be read from.
 */
@NotNullByDefault
public interface Resource {

    /**
     * Describes the resources as a URI. How a resource is described as a URI heavily depends on the
     * type of resource and is not specified any further. A resource specifically doesn't need to include
     * all the data that identifies the resource in this URI.
     */
    URI describe();

    /**
     * Opens an {@link InputStream} to read from this resource.
     */
    InputStream openStream() throws IOException;

    /**
     * Opens a {@link Reader} to read from this resource using the {@link StandardCharsets#UTF_8 utf8} charset.
     */
    default Reader openReader() throws IOException {
        return this.openReader(StandardCharsets.UTF_8);
    }

    /**
     * Opens a {@link Reader} to read from this resource.
     */
    default Reader openReader(Charset charset) throws IOException {
        return new InputStreamReader(this.openStream(), charset);
    }

    /**
     * Try to detect the content type (MIME type) of this resource.
     *
     * @return The detected content type or {@link Optional#empty()} if the content type could not be detected.
     */
    default Optional<String> probeContentType() throws IOException {
        return Optional.empty();
    }

    /**
     * Creates a resource tha treads from the given path.
     */
    static Resource of(Path path) {
        return new PathResource(path);
    }

    /**
     * Creates a resource tha treads from the given URL.
     */
    static Resource of(URL url) {
        return UrlResource.of(url);
    }
}
