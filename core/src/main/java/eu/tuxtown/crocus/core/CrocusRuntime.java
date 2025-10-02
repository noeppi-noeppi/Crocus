package eu.tuxtown.crocus.core;

import eu.tuxtown.crocus.api.attribute.Attribute;
import eu.tuxtown.crocus.core.loader.Services;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class CrocusRuntime {

    private static final AtomicReference<CrocusRuntime> instance = new AtomicReference<>(null);

    public static CrocusRuntime get() {
        CrocusRuntime rt = instance.get();
        if (rt != null) return rt;
        throw new IllegalStateException("The Crocus runtime has not yet been initialized.");
    }

    private final Path path;
    private final ModuleLayer pluginLayer;
    private final boolean verbose;
    private int logLayer;
    @Nullable private Services services;
    @Nullable private Map<String, Attribute<?>> attributes;

    public CrocusRuntime(Path path, ModuleLayer pluginLayer, boolean verbose) {
        this.path = path;
        this.pluginLayer = pluginLayer;
        this.verbose = verbose;
        this.logLayer = 0;
        this.services = null;
        this.attributes = null;
        if (!instance.compareAndSet(null, this)) {
            throw new IllegalStateException("The Crocus runtime has already been created.");
        }
    }

    public boolean verbose() {
        return this.verbose;
    }

    public Path path() {
        return this.path;
    }

    public ModuleLayer pluginLayer() {
        return this.pluginLayer;
    }

    public synchronized void initialize(Map<String, Attribute<?>> attributes, Services services) {
        if (this.attributes != null || this.services != null) {
            throw new IllegalStateException("Services have already been initialized.");
        }
        this.attributes = Map.copyOf(attributes);
        this.services = services;
    }

    public Map<String, Attribute<?>> attributes() {
        return this.attributes == null ? Map.of() : this.attributes;
    }

    public Services services() {
        return this.services == null ? Services.EMPTY : this.services;
    }

    public void log(String message, boolean debug) {
        if (debug && !this.verbose) return;
        String indent = this.logLayer <= 0 ? "" : "  ".repeat(this.logLayer - 1) + "- ";
        System.out.println(indent + message);
    }

    public void logWithCaller(@Nullable Class<?> caller, String message, boolean debug) {
        String moduleName = (caller == null || !caller.getModule().isNamed()) ? "<unknown>" : caller.getModule().getName();
        this.log(moduleName + ": " + message, debug);
    }

    public void increaseLogLayer() {
        this.logLayer += 1;
    }

    public void decreaseLogLayer() {
        this.logLayer = Math.max(0, this.logLayer - 1);
    }
}
