package eu.tuxtown.crocus.core.loader;

import bootstrap.api.LauncherConstants;
import bootstrap.api.ModuleSystem;
import bootstrap.jar.*;
import bootstrap.jar.classloading.ClassTransformer;
import bootstrap.jar.classloading.ModuleLoaderPool;

import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.Attributes;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PluginLoader {

    private static final Attributes.Name CROCUS_LAYER_ISOLATION = new Attributes.Name("Crocus-Layer-Isolation");

    // Crocus.info can't be used here as the runtimes does not yet exist.
    public static ModuleLoaderPool.Controller loadPlugins(ModuleSystem system, Path runPath) {
        System.out.println("Loading plugins");
        List<Path> pluginFiles = new ArrayList<>();
        try {
            List<Path> pluginDirs = new ArrayList<>(2);
            if (System.getProperty(LauncherConstants.PROP_HOME) != null) {
                pluginDirs.add(Path.of(System.getProperty(LauncherConstants.PROP_HOME)).resolve("lib/plugins"));
            }
            pluginDirs.add(runPath.resolve("plugins"));

            for (Path dir : pluginDirs) {
                if (Files.isDirectory(dir)) try (Stream<Path> plugins = Files.list(dir)) {
                    for (Path path : plugins.toList()) if (Files.isRegularFile(path)) {
                        pluginFiles.add(path);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load plugins", e);
        }
        return loadPluginsFrom(system, pluginFiles);
    }

    public static ModuleLoaderPool.Controller loadPluginsFrom(ModuleSystem system, List<Path> pluginFiles) {
        List<Plugin> plugins = pluginFiles.stream().map(path -> findPlugin(system, path)).toList();

        Map<String, String> moduleCluster = new HashMap<>();
        for (int i = 0; i < plugins.size(); i++) {
            for (ModuleReference ref : plugins.get(i).finder().findAll()) {
                if (moduleCluster.put(ref.descriptor().name(), Integer.toString(i)) != null) {
                    throw new IllegalStateException("Duplicate module name after remap: " + ref.descriptor().name());
                }
            }
        }

        List<ModuleLayer> parentLayers = Stream.concat(Stream.of(system.layer()), plugins.stream().map(Plugin::layer).flatMap(Optional::stream)).toList();
        ModuleFinder compositeFinder = ModuleFinder.compose(plugins.stream().map(Plugin::finder).toArray(ModuleFinder[]::new));
        Configuration configuration = pluginConfiguration(system, compositeFinder, parentLayers);

        return ModuleLoaderPool.define("plugin", configuration, parentLayers, ClassTransformer.noop(), moduleCluster::get);
    }

    private static Plugin findPlugin(ModuleSystem system, Path path) {
        try {
            Jar mainJar = Jar.of(path);
            if (mainJar.descriptor().isAutomatic()) {
                throw new IllegalStateException("Crocus plugins can't be automatic modules: " + mainJar.name());
            }

            List<Jar> classpathJars = new ArrayList<>();
            Map<String, String> moduleRemap = new HashMap<>();

            Path classpathDir = mainJar.getPath("META-INF", "classpath");
            if (Files.isDirectory(classpathDir)) try (Stream<Path> cp = Files.list(classpathDir)) {
                for (Path nestedPath : cp.toList()) if (Files.isRegularFile(nestedPath)) {
                    Jar nestedJar = Jar.of(JarMetadataFilters.fileInferredModuleName(), nestedPath);
                    classpathJars.add(nestedJar);
                    if (moduleRemap.put(nestedJar.name(), mainJar.name() + "." + nestedJar.name()) != null) {
                        throw new IllegalStateException("Duplicate module name " + nestedJar.name() + " in plugin " + path);
                    }
                }
            }

            JarMetadataFilter metadataFilter = JarMetadataFilters.remapModuleNames(moduleRemap);

            Jar patchedMainJar = Jar.patch(mainJar, metadataFilter);
            List<Jar> patchedClasspathJars = new ArrayList<>(classpathJars.size());
            for (Jar jar : classpathJars) patchedClasspathJars.add(Jar.patch(jar, metadataFilter));
            List<Jar> allPatchedJars = Stream.concat(Stream.of(patchedMainJar), patchedClasspathJars.stream()).toList();

            if (Boolean.parseBoolean(patchedMainJar.manifest().getMainAttributes().getValue(CROCUS_LAYER_ISOLATION))) {
                // With layer isolation, we built upon the boot layer.
                ModuleFinder finder = JarModuleFinder.of(patchedClasspathJars);
                Configuration configuration = pluginConfiguration(system, finder, List.of(system.bootLayer()));
                ModuleLoaderPool.Controller controller = ModuleLoaderPool.defineWithOneLoader(
                        "plugin-" + mainJar.name().replace('.', '-'),
                        configuration, List.of(system.bootLayer()),
                        ClassTransformer.noop()
                );
                return new Plugin(JarModuleFinder.of(patchedMainJar), Optional.of(controller.layer()));
            } else {
                List<String> automaticModules = allPatchedJars.stream()
                        .map(Jar::descriptor)
                        .filter(ModuleDescriptor::isAutomatic)
                        .map(ModuleDescriptor::name)
                        .sorted().toList();
                if (!automaticModules.isEmpty()) {
                    throw new LayerInstantiationException("Plugin " + mainJar.name() + " deploys automatic modules in its classpath but has layer isolation turned off: " + String.join(", ", automaticModules));
                }

                JarModuleFinder finder = JarModuleFinder.of(allPatchedJars);
                return new Plugin(finder, Optional.empty());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct plugin", e);
        }
    }

    private static Configuration pluginConfiguration(ModuleSystem system, ModuleFinder finder, List<ModuleLayer> parents) {
        Set<String> nonReplaceableModules = Stream.concat(
                system.bootLayer().modules().stream(),
                Stream.of(PluginLoader.class.getModule())
        ).filter(Module::isNamed).map(Module::getName).collect(Collectors.toUnmodifiableSet());
        SplitModuleFinder splitFinder = SplitModuleFinder.of(finder, moduleName -> !nonReplaceableModules.contains(moduleName));
        return splitFinder.resolveAndBind(parents.stream().map(ModuleLayer::configuration).toList());
    }

    private record Plugin(JarModuleFinder finder, Optional<ModuleLayer> layer) {}
}
