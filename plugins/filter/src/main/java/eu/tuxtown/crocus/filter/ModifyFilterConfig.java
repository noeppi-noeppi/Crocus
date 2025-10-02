package eu.tuxtown.crocus.filter;

import eu.tuxtown.crocus.api.delegate.DelegateConfiguration;
import org.jetbrains.annotations.NotNullByDefault;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

@NotNullByDefault
public class ModifyFilterConfig {

    private final SimpleFilterType simpleType;

    private final List<SimpleFilter> when;
    private UnaryOperator<String> name;
    private UnaryOperator<String> description;
    private UnaryOperator<String> location;
    private UnaryOperator<URI> url;

    public ModifyFilterConfig(SimpleFilterType simpleType) {
        this.simpleType = simpleType;
        this.when = new ArrayList<>();
        this.name = UnaryOperator.identity();
        this.description = UnaryOperator.identity();
        this.location = UnaryOperator.identity();
        this.url = UnaryOperator.identity();
    }

    public void when(DelegateConfiguration config) {
        this.when.add(config.configure(this.simpleType));
    }

    public void name(UnaryOperator<String> name) {
        this.name = name;
    }

    public void description(UnaryOperator<String> description) {
        this.description = description;
    }

    public void location(UnaryOperator<String> location) {
        this.location = location;
    }

    public void url(UnaryOperator<URI> url) {
        this.url = url;
    }

    public List<SimpleFilter> getWhen() {
        return Collections.unmodifiableList(this.when);
    }

    public UnaryOperator<String> getName() {
        return this.name;
    }

    public UnaryOperator<String> getDescription() {
        return this.description;
    }

    public UnaryOperator<String> getLocation() {
        return this.location;
    }

    public UnaryOperator<URI> getUrl() {
        return this.url;
    }
}
