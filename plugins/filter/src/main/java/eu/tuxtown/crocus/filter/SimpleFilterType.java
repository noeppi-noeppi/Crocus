package eu.tuxtown.crocus.filter;

import eu.tuxtown.crocus.api.service.EventFilterType;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public class SimpleFilterType implements EventFilterType<SimpleFilter, SimpleFilterConfig> {

    @Override
    public String name() {
        return "simple";
    }

    @Override
    public SimpleFilterConfig createDelegate() {
        return new SimpleFilterConfig(this);
    }

    @Override
    public SimpleFilter create(SimpleFilterConfig delegate) {
        return new SimpleFilter(delegate);
    }
}
