package eu.tuxtown.crocus.google.request;

import com.google.api.client.util.BackOff;
import org.jetbrains.annotations.Contract;

@FunctionalInterface
public interface BackOffStrategy {

    BackOffStrategy FAIL = () -> BackOff.STOP_BACKOFF;

    @Contract(value ="-> new", pure = true)
    BackOff newBackOff();
}
