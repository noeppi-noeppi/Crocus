package eu.tuxtown.crocus.core.io;

import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.calendar.EventFilter;
import eu.tuxtown.crocus.core.CrocusRuntime;
import eu.tuxtown.crocus.core.Main;
import eu.tuxtown.crocus.core.configuration.SystemConfiguration;
import eu.tuxtown.crocus.core.dsl.ScriptEngine;
import eu.tuxtown.crocus.core.loader.Services;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@NotNullByDefault
public class IoMain {

    @SuppressWarnings("ConfusingMainMethod")
    public static void main(OptionParser options, Main.Action action, String[] args) throws IOException {
        OptionSpec<Path> specLoad = options.accepts("load", "Load an event dump from a file.").withRequiredArg().withValuesConvertedBy(new PathConverter());
        OptionSpec<Path> specDump = options.accepts("dump", "Dump the processed events in a file.").withRequiredArg().withValuesConvertedBy(new PathConverter());
        OptionSpec<String> specSource = options.accepts("source", "Load events from a source in the system configuration.").withRequiredArg();
        OptionSpec<String> specCalendar = options.accepts("calendar", "Load events from the system configuration that would have been put into the given calendar.").withRequiredArg();
        OptionSpec<String> specSink = options.accepts("sink", "Sync events to a system calendar.").withRequiredArg();
        OptionSpec<String> specISink = options.accepts("isink", "Sync events to a system calendar (incremental).").withRequiredArg();
        OptionSpec<String> specFilter = options.accepts("filter", "Add an event filter. [plugin:]id[@ns]=code").withRequiredArg();

        if (action instanceof Main.Action.ShowHelp) {
            options.printHelpOn(System.out);
        } else if (action instanceof Main.Action.Run(SystemConfiguration systemConfig)) {
            OptionSet set = options.parse(args);

            List<IoSource> sources = new ArrayList<>();
            List<IoFilter> filters = new ArrayList<>();
            List<IoSink> sinks = new ArrayList<>();

            for (Path path : set.valuesOf(specLoad)) sources.add(new IoSource.File(path));
            for (Path path : set.valuesOf(specDump)) sinks.add(new IoSink.File(path));
            for (String source : set.valuesOf(specSource)) sources.add(new IoSource.SystemSource(source));
            for (String calendar : set.valuesOf(specCalendar)) sources.add(new IoSource.SystemCalendar(calendar));
            for (String sink : set.valuesOf(specSink)) sinks.add(new IoSink.SystemSink(sink, false));
            for (String sink : set.valuesOf(specISink)) sinks.add(new IoSink.SystemSink(sink, true));
            for (String filter : set.valuesOf(specFilter)) filters.add(parseFilter(CrocusRuntime.get().services(), filter));

            IoRunner.run(systemConfig, sources, filters, sinks);
        }
    }

    private static IoFilter parseFilter(Services services, String filter) {
        if (!filter.contains("=")) throw new IllegalArgumentException("Invalid filter string: '" + filter + "'");
        String cfg = filter.substring(0, filter.indexOf('='));
        String code = filter.substring(filter.indexOf('=') + 1);
        String id;
        @Nullable String ns;
        if (cfg.contains("@")) {
            id = cfg.substring(0, cfg.indexOf('@'));
            ns = cfg.substring(cfg.indexOf('@') + 1);
        } else {
            id = cfg;
            ns = null;
        }
        EventFilter builtFilter = ScriptEngine.configureFromString(services.resolve(Crocus.ServiceDomain.EVENT_FILTER, id).instance(), code);
        return new IoFilter(ns, builtFilter);
    }
}
