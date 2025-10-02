package eu.tuxtown.crocus.google.api;

import eu.tuxtown.crocus.api.attribute.Attribute;
import eu.tuxtown.crocus.api.attribute.AttributeAdapters;
import eu.tuxtown.crocus.api.attribute.Attributes;
import eu.tuxtown.crocus.api.attribute.DefaultedAttribute;
import eu.tuxtown.crocus.api.service.AttributeProvider;
import org.jetbrains.annotations.NotNullByDefault;

import java.time.ZoneId;

@NotNullByDefault
public class GoogleAttributes implements AttributeProvider {

    public static final Attribute<ZoneId> TIMEZONE_OVERRIDE = Attributes.create("google_timezone_override", ZoneId.class, AttributeAdapters.TIMEZONE);
    public static final DefaultedAttribute<Boolean> BLOCKS_TIME = Attributes.create("google_blocks_time", boolean.class, AttributeAdapters.BOOLEAN, false);

    @Override
    public void registerAttributes(Registry registry) {
        registry.register(TIMEZONE_OVERRIDE);
        registry.register(BLOCKS_TIME);
    }
}
