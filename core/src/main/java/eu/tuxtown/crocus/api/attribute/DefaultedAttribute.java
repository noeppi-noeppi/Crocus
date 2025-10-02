package eu.tuxtown.crocus.api.attribute;

import org.jetbrains.annotations.NotNullByDefault;

/**
 * A non-standard event attribute with a default value.
 */
@NotNullByDefault
public sealed interface DefaultedAttribute<T> extends Attribute<T> permits Attributes.DefaultedImpl {

    /**
     * The default value of the attribute.
     */
    T defaultValue();
}
