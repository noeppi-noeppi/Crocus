package eu.tuxtown.crocus.core.loader;

import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.attribute.Attribute;
import eu.tuxtown.crocus.api.service.AttributeProvider;
import eu.tuxtown.crocus.core.CrocusRuntime;
import eu.tuxtown.crocus.core.configuration.SystemConfiguration;
import eu.tuxtown.crocus.core.dsl.ScriptEngine;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SystemLoader {

    public static SystemConfiguration load(CrocusRuntime runtime) throws IOException {
        Crocus.info("Loading attributes");
        Map<String, Attribute<?>> attributes = loadAttributes(runtime);

        Crocus.info("Loading system secrets");
        Properties secrets;
        CrocusRuntime.get().increaseLogLayer();
        try {
            secrets = loadSecrets(runtime);
        } finally {
            CrocusRuntime.get().decreaseLogLayer();
        }

        Path systemConfigPath = runtime.path().resolve("config.groovy");
        if (!Files.isRegularFile(systemConfigPath)) {
            throw new FileNotFoundException(systemConfigPath.getFileName() + " not found.");
        }

        Crocus.info("Loading system configuration");
        Services services;
        CrocusRuntime.get().increaseLogLayer();
        try {
            services = new Services(runtime.pluginLayer());
            runtime.initialize(attributes, services);
        } finally {
            CrocusRuntime.get().decreaseLogLayer();
        }

        return ScriptEngine.loadConfig(systemConfigPath, services, secrets);
    }

    private static Map<String, Attribute<?>> loadAttributes(CrocusRuntime runtime) {
        record AttributeEntry(String module, Attribute<?> attribute) {}
        AtomicBoolean failed = new AtomicBoolean(false); // Detect when a plugin catches the exception.
        Map<String, AttributeEntry> attributeMap = new HashMap<>();
        for (ServiceLoader.Provider<AttributeProvider> provider : ServiceLoader.load(runtime.pluginLayer(), AttributeProvider.class).stream().toList()) {
            String moduleName = Objects.requireNonNull(provider.type().getModule().getName(), "ALL-UNNAMED");
            AttributeProvider.Registry registry = attribute -> {
                AttributeEntry newEntry = new AttributeEntry(moduleName, attribute);
                AttributeEntry oldEntry = attributeMap.put(attribute.name(), newEntry);
                if (oldEntry != null) {
                    failed.set(true);
                    throw new IllegalStateException("Multiple modules provide the same attribute" + attribute.name() + ": " + oldEntry.module() + ", " + newEntry.module());
                }
            };
            provider.get().registerAttributes(registry);
        }
        if (failed.get()) {
            throw new IllegalStateException("A plugin did something funky and swallowed an exception while registering attributes.");
        }
        return attributeMap.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().attribute()));
    }

    private static Properties loadSecrets(CrocusRuntime runtime) throws IOException {
        Path secretFile = runtime.path().resolve("secrets.properties");
        Path secretDir = runtime.path().resolve("secrets");
        Properties secrets = new Properties();
        if (Files.isRegularFile(secretFile)) {
            try (BufferedReader reader = Files.newBufferedReader(secretFile)) {
                secrets.load(reader);
            }
        } else {
            Crocus.info(secretFile.getFileName() + " not found.");
        }
        if (Files.isDirectory(secretDir)) {
            try (Stream<Path> paths = Files.list(secretDir)) {
                for (Path path : paths.toList()) {
                    String name = path.getFileName() == null ? null : path.getFileName().toString();
                    if (name != null) secrets.setProperty(name, Files.readString(path, StandardCharsets.UTF_8).strip());
                }
            }
        } else {
            Crocus.info(secretFile.getFileName() + " directory not found.");
        }
        return secrets;
    }
}
