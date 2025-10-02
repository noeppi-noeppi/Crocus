package eu.tuxtown.crocus.core.dsl.meta;

import eu.tuxtown.crocus.api.delegate.DelegateConfigurable;
import eu.tuxtown.crocus.api.delegate.DelegateConfiguration;
import eu.tuxtown.crocus.core.dsl.ScriptEngine;
import groovy.lang.Closure;
import org.jetbrains.annotations.NotNullByDefault;

// Note: DelegateConfiguration may not be a functional interface, otherwise groovy won't call our type transform.
@NotNullByDefault
@SuppressWarnings("ClassCanBeRecord")
public final class DelegateConfigurationImpl implements DelegateConfiguration {

    private final Closure<?> closure;

    public DelegateConfigurationImpl(Closure<?> closure) {
        this.closure = closure;
    }

    @Override
    public <T> T configure(DelegateConfigurable<T, ?> configurable) {
        return ScriptEngine.configure(configurable, this.closure);
    }

    @Override
    public DelegateConfiguration andThen(DelegateConfiguration other) {
        if (other instanceof DelegateConfigurationImpl impl) {
            return new DelegateConfigurationImpl(this.closure.andThen(impl.closure));
        } else {
            throw new IllegalStateException("Custom implementations of DelegateConfiguration are unsupported");
        }
    }
}
