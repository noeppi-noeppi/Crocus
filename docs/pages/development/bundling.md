# Plugin Bundling

Many plugins require additional libraries to operate.
Therefore, plugins can bundle additional dependencies.
When Crocus loads a plugin, it looks for a directory named `META-INF/classpath` inside the plugin *Jar*-file.
Every *Jar*-file contained in there is loaded as a module to plugin module layer.

The dependencies of a plugin get a changed module name at runtime to not interfere with dependencies provided by other
 plugins.
It is even possible that multiple plugins bundle different versions of the same library.
In this case, each plugin will see its own version of the library independent of the other version that is also present
 in the plugin module layer.

Plugins can depend on other plugins to be present.
Such a dependency is expressed by a `requires` directive in the plugins module descriptor.

## Layer Isolation

The standard approach of remapping module module names of bundles *Jar*-files is not sufficient for automatic modules.
Automatic modules always read every other module and would therefore interfere with the dependencies provided by other
 plugins.
To circumvent this, Crocus can load all dependencies bundles with a plugin into a separate module layer that then
 becomes a parent layer of the plugin layer.
This has the benefit of limiting automatic modules to a single plugin.
However, it also comes with a disadvantage: Dependencies bundled with that plugin can't depend on other plugins any
 more.

Layer isolation can be enabled for a plugin by setting an attribut in the plugins `MANIFEST.MF`:
```manifest
Crocus-Layer-Isolation: true
```
In general, its advised to only enable layer isolation if there are *Jar*-files without an explicit module descriptor
 in `MMETA-INF/classpath`.
In that case it is also *required* to enabled layer isolation as Crocus refuses to load automatic modules without it.

## Gradle Buildscript Example

This section shows an example of a [Gradle](https://gradle.org/) buildscript that enables dependency bundling.
Dependencies that should be bundled can be added to the `classpath` configuration.

```groovy
plugins {
    'java-library'
}

var cpDependencies = configurations.register('classpath') { cf ->
    cf.canBeResolved = false
    cf.canBeConsumed = false
}
var cpElements = configurations.register('classpathElements') { cf ->
    cf.canBeResolved = true
    cf.canBeConsumed = false
    cf.extendsFrom(cpDependencies.get())
}
configurations.named('implementation').configure { it.extendsFrom(cpDependencies.get()) }

dependencies {
    //classpath 'group:name:version' (1)
}

tasks.named('jar', Jar).configure {
    // uncomment to enable layer isolation (2)
    //manifest.attributes('Crocus-Layer-Isolation': true)
    from(cpElements) {
        into('META-INF/classpath')
    }
}
```

1. All dependencies added via `classpath` are bundled.
   Dependencies that should not be bundled, should use `api` instead.
   This for example includes the Crocus core or additional plugins that this plugin depends on.
2. Uncommenting the following line adds the `Crocus-Layer-Isolation` attribute to the jar manifest and therefore enables
    [layer isolation](#layer-isolation).
   This is required when bundling automatic modules.
