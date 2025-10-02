package eu.tuxtown.crocus.google.request;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;

public class ServerErrorRetryHandler implements HttpUnsuccessfulResponseHandler {

    public static final ServerErrorRetryHandler INSTANCE = new ServerErrorRetryHandler();

    public static final HttpRequestInitializer INITIALIZER = req -> {
        req.setUnsuccessfulResponseHandler(INSTANCE);
        req.setNumberOfRetries(3);
    };

    private ServerErrorRetryHandler() {}

    @Override
    public boolean handleResponse(HttpRequest request, HttpResponse response, boolean supportsRetry) {
        return supportsRetry && response.getStatusCode() >= 500 && response.getStatusCode() < 600;
    }
}
