package eu.tuxtown.crocus.filter;

import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.service.EventFilterType;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public class ModifyFilterType implements EventFilterType<ModifyFilter, ModifyFilterConfig> {

    @Override
    public String name() {
        return "modify";
    }

    @Override
    public ModifyFilterConfig createDelegate() {
        return new ModifyFilterConfig(Crocus.serviceInstance(Crocus.ServiceDomain.EVENT_FILTER, SimpleFilterType.class));
    }

    @Override
    public ModifyFilter create(ModifyFilterConfig delegate) {
        return new ModifyFilter(delegate);
    }
}
