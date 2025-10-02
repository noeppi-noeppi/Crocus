package eu.tuxtown.crocus.api.attribute;

import org.jetbrains.annotations.NotNullByDefault;

/**
 * A non-standard event attribute.
 */
@NotNullByDefault
public sealed interface Attribute<T> extends Comparable<Attribute<?>> permits DefaultedAttribute, Attributes.Impl {

    /**
     * The name of the attribute. Must be unique.
     */
    String name();

    /**
     * The adapter for reading and writing this attribute.
     */
    AttributeAdapter<T> adapter();
}
