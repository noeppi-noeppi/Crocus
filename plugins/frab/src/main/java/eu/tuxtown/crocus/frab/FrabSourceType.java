package eu.tuxtown.crocus.frab;

import eu.tuxtown.crocus.api.service.EventSourceType;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public class FrabSourceType implements EventSourceType<FrabSource, FrabConfig> {

    @Override
    public String name() {
        return "frab";
    }

    @Override
    public FrabConfig createDelegate() {
        return new FrabConfig();
    }

    @Override
    public FrabSource create(FrabConfig delegate) {
        return new FrabSource(delegate);
    }
}
