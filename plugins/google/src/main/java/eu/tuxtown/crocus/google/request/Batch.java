package eu.tuxtown.crocus.google.request;

import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClient;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.util.BackOff;
import eu.tuxtown.crocus.api.Crocus;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.util.*;

@NotNullByDefault
public class Batch {

    private final AbstractGoogleJsonClient client;
    private final List<HandledRequest<?>> requests;
    private final BackOff backOff;

    public Batch(AbstractGoogleJsonClient client, List<HandledRequest<?>> requests, BackOff backOff) {
        this.client = client;
        this.requests = List.copyOf(requests);
        this.backOff = backOff;
    }

    public void send() throws IOException {
        this.backOff.reset();
        Set<UUID> returnedRequests = new HashSet<>();
        List<HandledRequest<?>> nextBatch = this.requests;
        int attempt = 0;
        while (!nextBatch.isEmpty()) {
            attempt += 1;
            BatchRequest batch = this.client.batch(ServerErrorRetryHandler.INITIALIZER);
            List<HandledRequest<?>> deferredRequests = new ArrayList<>();
            List<IOException> capturedExceptions = new ArrayList<>();
            for (HandledRequest<?> request : nextBatch) {
                insertIntoBatch(batch, request, returnedRequests, deferredRequests, capturedExceptions);
            }
            batch.execute();

            if (!capturedExceptions.isEmpty()) {
                IOException root = new IOException("Multiple failures in batch request.");
                root.fillInStackTrace();
                for (IOException captured : capturedExceptions) {
                    root.addSuppressed(captured);
                }
                throw root;
            }

            if (!deferredRequests.isEmpty()) {
                long millis = this.backOff.nextBackOffMillis();
                if (millis == BackOff.STOP) {
                    throw new IOException("BackOff: Giving up after " + attempt + " attempts. Missing " + deferredRequests.size() + " requests in a batch of " + this.requests.size());
                } else if (millis > 0) {
                    Crocus.debug(String.format(Locale.ROOT, "BackOff: Waiting for %1.3f seconds due to rate limiting. There are %d requests left in a batch of %d", millis / 1000d, deferredRequests.size(), this.requests.size()));
                    RequestExecutor.sleepFor(millis);
                }
            }

            nextBatch = Collections.unmodifiableList(deferredRequests);
        }
        if (returnedRequests.size() != this.requests.size()) {
            long missing = this.requests.stream()
                    .filter(req -> !returnedRequests.contains(req.uid()))
                    .count();
            throw new IOException("Not all requests were returned from the API: Missing " + missing + " requests in a batch of " + this.requests.size());
        }
    }

    private static <T> void insertIntoBatch(BatchRequest batch, HandledRequest<T> request, Set<UUID> returnedRequests, List<HandledRequest<?>> deferredRequests, List<IOException> capturedExceptions) throws IOException {
        request.request().queue(batch, new JsonBatchCallback<>() {

            @Override
            public void onSuccess(T result, HttpHeaders headers) {
                if (returnedRequests.add(request.uid())) {
                    Optional<IOException> ex = request.handleSuccess(result, headers);
                    ex.ifPresent(capturedExceptions::add);
                } else {
                    IOException ex = new IOException("Request returned twice: " + request.uid());
                    ex.fillInStackTrace();
                    capturedExceptions.add(ex);
                }
            }

            @Override
            public void onFailure(GoogleJsonError error, HttpHeaders headers) {
                if (RequestExecutor.backOffRequired(error)) {
                    deferredRequests.add(request);
                } else if (returnedRequests.add(request.uid())) {
                    returnedRequests.add(request.uid());
                    Optional<IOException> ex = request.handleFailure(error, headers);
                    ex.ifPresent(capturedExceptions::add);
                } else {
                    IOException ex = new IOException("Request returned twice: " + request.uid());
                    ex.fillInStackTrace();
                    capturedExceptions.add(ex);
                }
            }
        });
    }
}
