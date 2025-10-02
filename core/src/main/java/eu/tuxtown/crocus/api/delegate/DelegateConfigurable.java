package eu.tuxtown.crocus.api.delegate;

import org.jetbrains.annotations.NotNullByDefault;

/**
 * Something that can create configured objects via a delegate.
 *
 * @param <T> The type of object, this {@link DelegateConfigurable delegate configurable} creates.
 * @param <D> The type of the delegates, this {@link DelegateConfigurable delegate configurable} uses.
 */
@NotNullByDefault
public interface DelegateConfigurable<T, D> {

    /**
     * Creates a new configuration object that serves as delegate for the configuration closure.
     */
    D createDelegate();

    /**
     * Creates a new object based on the given configuration delegate.
     */
    T create(D delegate);
}
