package eu.tuxtown.crocus.api.service;

import eu.tuxtown.crocus.api.attribute.Attribute;
import org.jetbrains.annotations.NotNullByDefault;

/**
 * Plugin service class to register custom {@link Attribute attributes}.
 */
@NotNullByDefault
public interface AttributeProvider {

    /**
     * Invoked on attribute registration.
     */
    void registerAttributes(Registry registry);

    @NotNullByDefault
    interface Registry {

        /**
         * Registers a new non-standard attribute. All non-standard attributes must be registered, when the last plugin
         * service has been loaded.
         */
        void register(Attribute<?> attribute);
    }
}
