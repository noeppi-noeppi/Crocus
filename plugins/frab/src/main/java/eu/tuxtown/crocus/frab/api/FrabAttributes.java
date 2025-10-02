package eu.tuxtown.crocus.frab.api;

import eu.tuxtown.crocus.api.attribute.Attribute;
import eu.tuxtown.crocus.api.attribute.AttributeAdapters;
import eu.tuxtown.crocus.api.attribute.Attributes;
import eu.tuxtown.crocus.api.attribute.DefaultedAttribute;
import eu.tuxtown.crocus.api.service.AttributeProvider;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;

@NotNullByDefault
public class FrabAttributes implements AttributeProvider {

    public static final Attribute<String> ROOM = Attributes.create("frab_room", String.class, AttributeAdapters.STRING);
    public static final DefaultedAttribute<String> TYPE = Attributes.create("frab_type", String.class, AttributeAdapters.STRING, "Event");
    public static final DefaultedAttribute<List<String>> PEOPLE = Attributes.createList("frab_people", String.class, AttributeAdapters.STRING);

    @Override
    public void registerAttributes(Registry registry) {
        registry.register(ROOM);
        registry.register(TYPE);
        registry.register(PEOPLE);
    }
}
