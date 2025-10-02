package eu.tuxtown.crocus.google.request;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonErrorContainer;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClient;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.util.BackOff;
import com.google.api.client.util.ExponentialBackOff;
import eu.tuxtown.crocus.api.Crocus;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@NotNullByDefault
@SuppressWarnings("ClassCanBeRecord")
public class RequestExecutor {

    private static final int MAX_BATCH_SIZE = 100;
    private static final Set<Integer> USAGE_LIMIT_ERROR_CODES = Set.of(403, 429);
    private static final String USAGE_LIMIT_ERROR_DOMAIN = "usageLimits";

    public static final Set<Integer> NOT_FOUND = Set.of(404, 410);

    private final AbstractGoogleJsonClient client;
    private final BackOffStrategy backOffStrategy;

    public RequestExecutor(AbstractGoogleJsonClient client, BackOffStrategy backOffStrategy) {
        this.client = client;
        this.backOffStrategy = backOffStrategy;
    }

    public <T> T execute(AbstractGoogleJsonClientRequest<T> request) throws IOException {
        AtomicReference<@Nullable T> ref = new AtomicReference<>(null);
        this.sendSingleRequest(HandledRequest.of(request).success(ref::set).build());
        //noinspection DataFlowIssue
        return ref.get(); // nullable if T is void
    }

    public <T> Optional<T> tryExecute(AbstractGoogleJsonClientRequest<T> request, Set<Integer> ignoredErrorCodes) throws IOException {
        AtomicBoolean failure = new AtomicBoolean(false);
        AtomicReference<@Nullable T> ref = new AtomicReference<>(null);
        this.sendSingleRequest(HandledRequest.of(request).success(ref::set).failure(ignoredErrorCodes, err -> failure.set(true)).build());
        if (failure.get()) return Optional.empty();
        if (ref.get() == null) throw new IOException("Request was not answered. This should not happen.");
        return Optional.of(Objects.requireNonNull(ref.get()));
    }

    public void executeAll(HandledRequest<?>... requests) throws IOException {
        this.executeAll(Arrays.asList(requests));
    }

    public void executeAll(List<HandledRequest<?>> requests) throws IOException {
        int processedRequests = 0;
        while (processedRequests < requests.size()) {
            int nextBatchSize = Math.min(MAX_BATCH_SIZE, requests.size() - processedRequests);
            List<HandledRequest<?>> nextBatchView = requests.subList(processedRequests, processedRequests + nextBatchSize);
            this.sendSingleBatch(nextBatchView);
            processedRequests += nextBatchSize;
        }
    }

    public Batcher newBatcher() {
        return new Batcher();
    }

    private void sendSingleBatch(List<HandledRequest<?>> requests) throws IOException {
        if (requests.isEmpty()) return;
        if (requests.size() == 1) {
            this.sendSingleRequest(requests.getFirst());
        } else {
            new Batch(this.client, requests, this.backOffStrategy.newBackOff()).send();
        }
    }

    private <T> void sendSingleRequest(HandledRequest<T> request) throws IOException {
        BackOff backOff = this.backOffStrategy.newBackOff();
        backOff.reset();
        int attempt = 0;
        while (true) {
            attempt += 1;
            HttpResponse response = request.request().buildHttpRequest()
                    .setUnsuccessfulResponseHandler(ServerErrorRetryHandler.INSTANCE)
                    .setThrowExceptionOnExecuteError(false)
                    .execute();
            if (response.isSuccessStatusCode()) {
                Optional<IOException> ex = request.handleSuccess(response.parseAs(request.request().getResponseClass()), response.getHeaders());
                if (ex.isPresent()) throw ex.get();
                return;
            } else {
                GoogleJsonErrorContainer errorContainer = response.parseAs(GoogleJsonErrorContainer.class);
                if (!backOffRequired(errorContainer.getError())) {
                    Optional<IOException> ex = request.handleFailure(errorContainer.getError(), response.getHeaders());
                    if (ex.isPresent()) throw ex.get();
                    return;
                }
            }
            long millis = backOff.nextBackOffMillis();
            if (millis == BackOff.STOP) {
                throw new IOException("BackOff: Giving up after " + attempt + " attempts.");
            } else if (millis > 0) {
                Crocus.debug(String.format(Locale.ROOT, "BackOff: Waiting for %1.3f seconds due to rate limiting.", millis / 1000d));
                sleepFor(millis);
            }
        }
    }

    public static RequestExecutor getDefault(AbstractGoogleJsonClient client) {
        return new RequestExecutor(client, ExponentialBackOff::new);
    }

    static boolean backOffRequired(GoogleJsonError error) {
        boolean isUsageEErrorCode = USAGE_LIMIT_ERROR_CODES.contains(error.getCode());
        boolean hasUsageLimitsError = error.getErrors().stream().map(GoogleJsonError.ErrorInfo::getDomain).anyMatch(USAGE_LIMIT_ERROR_DOMAIN::equals);
        return isUsageEErrorCode && hasUsageLimitsError;
    }

    static void sleepFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            //
        }
    }

    public class Batcher implements AutoCloseable {

        private final List<HandledRequest<?>> currentRequests;

        private Batcher() {
            this.currentRequests = new ArrayList<>(RequestExecutor.MAX_BATCH_SIZE);
        }

        public void enqueue(HandledRequest.Builder<?> request) throws IOException {
            this.enqueue(request.build());
        }

        public void enqueue(HandledRequest<?> request) throws IOException {
            this.currentRequests.add(request);
            if (this.currentRequests.size() >= RequestExecutor.MAX_BATCH_SIZE) {
                List<HandledRequest<?>> batch = List.copyOf(this.currentRequests);
                this.currentRequests.clear();
                RequestExecutor.this.sendSingleBatch(batch);
            }
        }

        @Override
        public void close() throws IOException {
            List<HandledRequest<?>> batch = List.copyOf(this.currentRequests);
            this.currentRequests.clear();
            RequestExecutor.this.sendSingleBatch(batch);
        }
    }
}
