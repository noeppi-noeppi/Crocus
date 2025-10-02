package eu.tuxtown.crocus.google.request;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest;
import com.google.api.client.http.HttpHeaders;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

@NotNullByDefault
public final class HandledRequest<T> {

    private final UUID uid;
    private final AbstractGoogleJsonClientRequest<T> request;
    private final Handler<T> successHandler;
    private final Handler<GoogleJsonError> errorHandler;

    private HandledRequest(AbstractGoogleJsonClientRequest<T> request, Handler<T> successHandler, Handler<GoogleJsonError> errorHandler) {
        this.uid = UUID.randomUUID();
        this.request = request;
        if (request.getHttpContent() != null && !request.getHttpContent().retrySupported()) {
            throw new IllegalArgumentException("HandledRequest must support retry");
        }
        this.successHandler = successHandler;
        this.errorHandler = errorHandler;
    }

    public UUID uid() {
        return this.uid;
    }

    public AbstractGoogleJsonClientRequest<T> request() {
        return this.request;
    }

    public Optional<IOException> handleSuccess(@Nullable T result, HttpHeaders headers) {
        if (result == null && this.request().getResponseClass() != void.class && this.request().getResponseClass() != Void.class) {
            IOException ex = new FileNotFoundException("Empty response");
            ex.fillInStackTrace();
            return Optional.of(ex);
        }
        //noinspection DataFlowIssue
        return runHandler(this.successHandler, result, headers); // nullable if T is void
    }

    public Optional<IOException> handleFailure(@Nullable GoogleJsonError error, HttpHeaders headers) {
        if (error == null) {
            IOException ex = new IOException("Response with unsuccessful status code and no error description.");
            ex.fillInStackTrace();
            return Optional.of(ex);
        }
        return runHandler(this.errorHandler, error, headers);
    }

    private static <T> Optional<IOException> runHandler(Handler<T> handler, T result, HttpHeaders headers) {
        try {
            handler.handle(result, headers);
            return Optional.empty();
        } catch (IOException e) {
            return Optional.of(e);
        } catch (Exception e) {
            IOException ioex = new IOException("Request handler failure", e);
            ioex.fillInStackTrace();
            return Optional.of(ioex);
        }
    }

    public static <T> Builder<T> of(AbstractGoogleJsonClientRequest<T> request) {
        return new Builder<>(request);
    }

    @FunctionalInterface
    public interface Handler<T> {
        void handle(T result) throws IOException;

        default void handle(T result, HttpHeaders headers) throws IOException {
            this.handle(result);
        }
    }

    @NotNullByDefault
    public static final class Builder<T> {

        private final AbstractGoogleJsonClientRequest<T> request;
        @Nullable private Handler<T> successHandler;
        private final Map<Integer, Handler<GoogleJsonError>> codeErrorHandlers;
        @Nullable private Handler<GoogleJsonError> fallbackErrorHandler;

        private Builder(AbstractGoogleJsonClientRequest<T> request) {
            this.request = request;
            this.successHandler = null;
            this.codeErrorHandlers = new HashMap<>();
            this.fallbackErrorHandler = null;
        }

        public Builder<T> success(Handler<T> successHandler) {
            if (this.successHandler == null) {
                this.successHandler = successHandler;
            } else {
                this.successHandler = new CompositeHandler<>(List.of(this.successHandler, successHandler));
            }
            return this;
        }

        public Builder<T> failure(Handler<GoogleJsonError> errorHandler) {
            if (this.fallbackErrorHandler == null) {
                this.fallbackErrorHandler = errorHandler;
            } else {
                this.fallbackErrorHandler = new CompositeHandler<>(List.of(this.fallbackErrorHandler, errorHandler));
            }
            return this;
        }

        public Builder<T> failure(Set<Integer> codes, Handler<GoogleJsonError> errorHandler) {
            for (int errorCode : codes) {
                this.failure(errorCode, errorHandler);
            }
            return this;
        }

        public Builder<T> failure(int code, Handler<GoogleJsonError> errorHandler) {
            if (this.codeErrorHandlers.containsKey(code)) {
                this.codeErrorHandlers.put(code, new CompositeHandler<>(List.of(this.codeErrorHandlers.get(code), errorHandler)));
            } else {
                this.codeErrorHandlers.put(code, errorHandler);
            }
            return this;
        }

        public HandledRequest<T> build() {
            Handler<T> successHandler = Objects.requireNonNullElse(this.successHandler, ignored -> {});
            Handler<GoogleJsonError> errorHandler = new ErrorHandler(Map.copyOf(this.codeErrorHandlers), this.fallbackErrorHandler);
            return new HandledRequest<>(this.request, successHandler, errorHandler);
        }
    }

    @NotNullByDefault
    private record CompositeHandler<T>(List<Handler<T>> handlers) implements Handler<T> {

        public CompositeHandler(List<Handler<T>> handlers) {
            this.handlers = handlers.stream()
                    .flatMap(handler -> handler instanceof CompositeHandler<T>(List<Handler<T>> nested) ? nested.stream() : Stream.of(handler))
                    .toList();
        }

        @Override
        public void handle(T result) throws IOException {
            for (Handler<T> handler : this.handlers()) {
                handler.handle(result);
            }
        }

        @Override
        public void handle(T result, HttpHeaders headers) throws IOException {
            for (Handler<T> handler : this.handlers()) {
                handler.handle(result, headers);
            }
        }
    }

    @NotNullByDefault
    private record ErrorHandler(Map<Integer, Handler<GoogleJsonError>> codeHandlers, @Nullable Handler<GoogleJsonError> fallback) implements Handler<GoogleJsonError> {

        public ErrorHandler(Map<Integer, Handler<GoogleJsonError>> codeHandlers, @Nullable Handler<GoogleJsonError> fallback) {
            this.codeHandlers = Map.copyOf(codeHandlers);
            this.fallback = fallback;
        }

        @Override
        public void handle(GoogleJsonError error) throws IOException {
            if (this.codeHandlers.containsKey(error.getCode())) {
                this.codeHandlers.get(error.getCode()).handle(error);
            } else if (this.fallback != null) {
                this.fallback.handle(error);
            } else {
                this.defaultFailure(error);
            }
        }

        @Override
        public void handle(GoogleJsonError error, HttpHeaders headers) throws IOException {
            if (this.codeHandlers.containsKey(error.getCode())) {
                this.codeHandlers.get(error.getCode()).handle(error, headers);
            } else if (this.fallback != null) {
                this.fallback.handle(error, headers);
            } else {
                this.defaultFailure(error);
            }
        }

        private void defaultFailure(GoogleJsonError error) throws IOException {
            throw new IOException("Request failure: " + error.getCode() + " " + error.getMessage() + "\n" + error);
        }
    }
}
