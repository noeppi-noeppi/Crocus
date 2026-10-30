package eu.tuxtown.crocus.caldav.util;

import org.jetbrains.annotations.NotNullByDefault;

import java.net.URI;
import java.net.URISyntaxException;

@NotNullByDefault
public class ResourceHelper {

    public static URI normalizeURI(URI uri) {
        if (uri.isOpaque()) {
            throw new IllegalArgumentException("Opaque URI used as CalDAV resource: " + uri);
        }
        try {
            return new URI(
                    uri.getScheme(), null, uri.getHost(), uri.getPort(),
                    uri.getPath() + (uri.getPath().endsWith("/") ? "" : "/"),
                    uri.getQuery(), null
            );
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI used as CalDAV resource: " + uri, e);
        }
    }
}
