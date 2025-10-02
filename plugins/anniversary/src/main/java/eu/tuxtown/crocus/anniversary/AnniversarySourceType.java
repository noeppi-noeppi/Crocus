package eu.tuxtown.crocus.anniversary;

import eu.tuxtown.crocus.api.service.EventSourceType;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public class AnniversarySourceType implements EventSourceType<AnniversarySource, AnniversaryConfig> {

    @Override
    public String name() {
        return "anniversary";
    }

    @Override
    public AnniversaryConfig createDelegate() {
        return new AnniversaryConfig();
    }

    @Override
    public AnniversarySource create(AnniversaryConfig delegate) {
        return new AnniversarySource(delegate);
    }
}
