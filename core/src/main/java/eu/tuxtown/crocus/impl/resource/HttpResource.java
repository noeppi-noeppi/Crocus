package eu.tuxtown.crocus.impl.resource;

import eu.tuxtown.crocus.api.resource.Resource;
import eu.tuxtown.crocus.impl.ssl.SSLContextHelper;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.function.Consumer;

@NotNullByDefault
@SuppressWarnings("ClassCanBeRecord")
public class HttpResource implements Resource {

    private final URI uri;
    private final HttpClient client;
    private final List<Map.Entry<String, String>> headers;

    private HttpResource(URI uri, HttpClient client, List<Map.Entry<String, String>> headers) {
        this.uri = uri;
        this.client = client;
        this.headers = headers;
    }

    @Override
    public URI describe() {
        return this.uri;
    }

    @Override
    public InputStream openStream() throws IOException {
        return this.sendRequest(HttpRequest.Builder::GET, HttpResponse.BodyHandlers.ofInputStream()).body();
    }

    @Override
    public Optional<String> probeContentType() throws IOException {
        HttpResponse<Void> response = this.sendRequest(HttpRequest.Builder::HEAD, HttpResponse.BodyHandlers.discarding());
        return response.headers().firstValue("Content-Type");
    }

    @Override
    public String toString() {
        return this.uri.toString();
    }

    private <T> HttpResponse<T> sendRequest(Consumer<HttpRequest.Builder> method, HttpResponse.BodyHandler<T> bodyHandler) throws IOException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(this.uri);
        method.accept(requestBuilder);
        for (Map.Entry<String, String> entry : this.headers) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }
        HttpRequest request = requestBuilder.build();
        HttpResponse<T> response;
        try {
            response = this.client.send(request, bodyHandler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupt during HTTP request.", e);
        }
        if ((response.statusCode() / 100) != 2) {
            throw new IOException("Got HTTP response code " + response.statusCode() + ": " + this.uri);
        }
        return response;
    }

    public static Builder builder(URI uri) {
        return new Builder(uri);
    }

    public static class Builder {

        private final URI uri;
        private final List<Map.Entry<String, String>> headers;
        private final Map<String, PasswordAuthentication> authenticationMap;
        private final List<Resource> trustedCertificates;
        private boolean followRedirects;

        private Builder(URI uri) {
            this.uri = uri;
            if (!Objects.equals("http", this.uri.getScheme().toLowerCase(Locale.ROOT)) && !Objects.equals("https", this.uri.getScheme().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Invalid protocol for HTTP resources: " + uri.getScheme());
            }
            this.headers = new ArrayList<>();
            this.authenticationMap = new HashMap<>();
            this.trustedCertificates = new ArrayList<>();
            this.followRedirects = true;
        }

        public void header(String key, String value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            this.headers.add(Map.entry(key, value));
        }

        public void noRedirects() {
            this.followRedirects = false;
        }

        public void authenticate(String host, String username, String password) {
            Objects.requireNonNull(host);
            Objects.requireNonNull(username);
            Objects.requireNonNull(password);
            char[] passwordArray = password.toCharArray();
            this.authenticationMap.put(host, new PasswordAuthentication(username, passwordArray));
        }

        public void trust(Resource certificate) {
            Objects.requireNonNull(certificate);
            this.trustedCertificates.add(certificate);
        }

        public HttpResource build() {
            HttpClient.Builder httpBuilder = HttpClient.newBuilder();
            httpBuilder.followRedirects(this.followRedirects ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER);
            if (!this.authenticationMap.isEmpty()) {
                httpBuilder.authenticator(new BasicAuthenticator(this.authenticationMap));
            }
            if (!this.trustedCertificates.isEmpty()) try {
                SSLContext sslContext = SSLContextHelper.load(this.trustedCertificates);
                httpBuilder.sslContext(sslContext);
            } catch (IOException e) {
                throw new RuntimeException("Failed to set up ssl context for http resource", e);
            }
            HttpClient client = httpBuilder.build();
            return new HttpResource(this.uri, client, List.copyOf(this.headers));
        }
    }

    private static final class BasicAuthenticator extends Authenticator {

        private final Map<String, PasswordAuthentication> authenticationMap;

        private BasicAuthenticator(Map<String, PasswordAuthentication> authenticationMap) {
            this.authenticationMap = Map.copyOf(authenticationMap);
        }

        @Override
        protected @Nullable PasswordAuthentication getPasswordAuthentication() {
            return this.authenticationMap.get(this.getRequestingHost());
        }
    }
}
