package eu.tuxtown.crocus.builtin;

import eu.tuxtown.crocus.api.service.EventSourceType;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public class DumpSourceType implements EventSourceType<DumpSource, DumpConfig> {

    @Override
    public String name() {
        return "dump";
    }

    @Override
    public DumpConfig createDelegate() {
        return new DumpConfig();
    }

    @Override
    public DumpSource create(DumpConfig delegate) {
        return new DumpSource(delegate);
    }
}
