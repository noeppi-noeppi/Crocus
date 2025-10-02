package eu.tuxtown.crocus.impl.resource;

import eu.tuxtown.crocus.api.resource.Resource;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@NotNullByDefault
@SuppressWarnings("ClassCanBeRecord")
public class MemoryResource implements Resource {

    private final @Nullable String mime;
    private final byte[] data;

    public MemoryResource(byte[] data) {
        this.mime = null;
        this.data = new byte[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    public MemoryResource(String mime, byte[] data) {
        this.mime = Objects.requireNonNull(mime);
        this.data = new byte[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    @Override
    public URI describe() {
        try {
            String uriMime = Objects.requireNonNull(this.mime, "application/octet-stream");
            String base64 = Base64.getEncoder().encodeToString(this.data);
            return new URI("data", uriMime + ";base64," + base64, null);
        } catch (URISyntaxException e) {
            return URI.create("unknown:/");
        }
    }

    @Override
    public InputStream openStream() {
        return new ByteArrayInputStream(this.data);
    }

    @Override
    public Optional<String> probeContentType() {
        return Optional.ofNullable(this.mime);
    }

    @Override
    public String toString() {
        return "<data>";
    }

    public static MemoryResource of(URI uri) {
        if (!"data".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Expected a data URI, got " + uri.getScheme());
        }

        RuntimeException ex = new IllegalArgumentException("Invalid data URI: " + uri);

        String ssp = uri.getRawSchemeSpecificPart();
        int splitIdx = ssp.indexOf(',');
        if (splitIdx < 0) throw ex;

        String[] preamble = ssp.substring(0, splitIdx).split(";", -1);
        String encodedData = ssp.substring(splitIdx + 1);
        String mime = "text/plain";
        if (preamble.length > 0 && preamble[0].indexOf('=') < 0 && !preamble[0].isEmpty()) {
            mime = preamble[0].toLowerCase(Locale.ROOT);
        }
        Charset charset = StandardCharsets.US_ASCII;
        boolean useBase64 = false;
        for (String part : preamble) {
            if (part.toLowerCase(Locale.ROOT).startsWith("charset=")) {
                charset = Charset.forName(part.substring(8), StandardCharsets.US_ASCII);
            }
            if (part.toLowerCase(Locale.ROOT).equals("base64")) {
                useBase64 = true;
            }
        }

        byte[] data;
        if (useBase64) {
            try {
                data = Base64.getDecoder().decode(encodedData);
            } catch (IllegalArgumentException e) {
                ex.initCause(e);
                throw ex;
            }
        } else {
            String urlDecodedString;
            try {
                urlDecodedString = URLDecoder.decode(encodedData, charset);
            } catch (IllegalArgumentException e) {
                ex.initCause(e);
                throw ex;
            }
            ByteBuffer buf = StandardCharsets.UTF_8.encode(urlDecodedString);
            data = new byte[buf.remaining()];
            buf.get(data);
        }
        return new MemoryResource(mime, data);
    }
}
