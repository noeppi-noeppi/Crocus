package eu.tuxtown.crocus.core;

import bootstrap.api.ModuleSystem;
import bootstrap.jar.classloading.ModuleLoaderPool;
import bootstrap.spi.Entrypoint;
import eu.tuxtown.crocus.core.configuration.SystemConfiguration;
import eu.tuxtown.crocus.core.dsl.ScriptEngine;
import eu.tuxtown.crocus.core.io.IoMain;
import eu.tuxtown.crocus.core.loader.PluginLoader;
import eu.tuxtown.crocus.core.loader.SystemLoader;
import eu.tuxtown.crocus.core.sync.SyncMain;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Path;

@NotNullByDefault
public class Main implements Entrypoint {

    @Override
    public String name() {
        return "crocus";
    }

    @Override
    public void main(ModuleSystem system, String[] args) throws Throwable {
        OptionParser options = new OptionParser(false);
        options.allowsUnrecognizedOptions();
        OptionSpec<Void> specHelp = options.accepts("help").forHelp();
        OptionSpec<Path> specPath = options.accepts("path").withRequiredArg().withValuesConvertedBy(new PathConverter()).defaultsTo(Path.of("").toAbsolutePath());
        OptionSpec<Void> specVerbose = options.accepts("verbose");
        OptionSpec<Void> specIO = options.accepts("io");
        OptionSet set = options.parse(args);

        Path self = set.valueOf(specPath);

        OptionParser optionsDelegate = new OptionParser();
        optionsDelegate.accepts("help").forHelp();
        optionsDelegate.accepts("path", "The path, where Crocus stores its data.").withRequiredArg().withValuesConvertedBy(new PathConverter()).defaultsTo(self);
        optionsDelegate.accepts("io", "Enable io mode instead of performing a sync.");
        optionsDelegate.accepts("verbose", "Be more verbose.");

        Action action;
        if (set.has(specHelp)) {
            action = Action.ShowHelp.INSTANCE;
        } else {
            ModuleLoaderPool.Controller pluginController = PluginLoader.loadPlugins(system, self);
            CrocusRuntime runtime = new CrocusRuntime(self, pluginController.layer(), set.has(specVerbose));
            ScriptEngine.init(system, pluginController.layerController());
            SystemConfiguration config = SystemLoader.load(runtime);
            action = new Action.Run(config);
        }

        if (set.has(specIO)) {
            IoMain.main(optionsDelegate, action, args);
        } else {
            SyncMain.main(optionsDelegate, action, args);
        }
    }

    public sealed interface Action {
        enum ShowHelp implements Action { INSTANCE }
        record Run(SystemConfiguration config) implements Action {}
    }
}
