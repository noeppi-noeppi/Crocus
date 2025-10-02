package eu.tuxtown.crocus.impl.resource;

import eu.tuxtown.crocus.api.resource.Resource;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Optional;

@NotNullByDefault
public final class UrlResource implements Resource {

    private final URL url;

    private UrlResource(URL url) {
        this.url = url;
    }

    @Override
    public URI describe() {
        try {
            return this.url.toURI();
        } catch (URISyntaxException e) {
            return URI.create("unknown:/");
        }
    }

    @Override
    public InputStream openStream() throws IOException {
        return this.url.openStream();
    }

    @Override
    public Optional<String> probeContentType() throws IOException {
        URLConnection con = this.url.openConnection();
        con.connect();
        return Optional.ofNullable(con.getContentType());
    }

    @Override
    public String toString() {
        return this.url.toString();
    }

    public static Resource of(URL url) {
        try {
            // Convert http(s) URLs to HttpResource. This allows redirects from http to https but denies redirects from
            // https to http. file URLs are not converted to PathResource as the file protocol also supports FTP access
            // if the host is not local.
            return switch (url.getProtocol().toLowerCase(Locale.ROOT)) {
                case "http", "https" -> HttpResource.builder(url.toURI()).build();
                default -> new UrlResource(url);
            };
        } catch (URISyntaxException e) {
            return new UrlResource(url);
        }
    }
}
