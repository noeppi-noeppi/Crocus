package eu.tuxtown.crocus.ical.api;

import biweekly.property.Classification;
import eu.tuxtown.crocus.api.attribute.Attribute;
import eu.tuxtown.crocus.api.attribute.AttributeAdapters;
import eu.tuxtown.crocus.api.attribute.Attributes;
import eu.tuxtown.crocus.api.attribute.DefaultedAttribute;
import eu.tuxtown.crocus.api.service.AttributeProvider;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public class ICalAttributes implements AttributeProvider {

    public static final DefaultedAttribute<Integer> PRIORITY = Attributes.create("ical_priority", int.class, AttributeAdapters.INT, 0);
    public static final Attribute<String> STATUS = Attributes.create("ical_status", String.class, AttributeAdapters.STRING);
    public static final Attribute<String> COLOR = Attributes.create("ical_color", String.class, AttributeAdapters.STRING);
    public static final DefaultedAttribute<String> CLASSIFICATION = Attributes.create("ical_classification", String.class, AttributeAdapters.STRING, Classification.PUBLIC);

    @Override
    public void registerAttributes(Registry registry) {
        registry.register(PRIORITY);
        registry.register(STATUS);
        registry.register(COLOR);
        registry.register(CLASSIFICATION);
    }
}
