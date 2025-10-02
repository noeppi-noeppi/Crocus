package eu.tuxtown.crocus.core.loader;

import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.service.Nameable;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

@NotNullByDefault
public class Services {

    public static final Services EMPTY = new Services((Void) null);

    private final Object lock;
    @Nullable private final ModuleLayer layer;
    private final Map<Crocus.ServiceDomain<?>, Map<Key, Service<?>>> services;

    private Services(@Nullable Void v) {
        this.lock = new Object();
        this.layer = null;
        this.services = new HashMap<>();
    }

    public Services(ModuleLayer layer) {
        this.lock = new Object();
        this.layer = Objects.requireNonNull(layer);
        this.services = new HashMap<>();
    }

    public <T extends Nameable> Service<T> resolve(Crocus.ServiceDomain<T> domain, String identifier) {
        if (identifier.indexOf(':') >= 0) {
            Key key = new Key(identifier.substring(0, identifier.indexOf(':')), identifier.substring(identifier.indexOf(':') + 1));
            return this.get(domain, key);
        } else {
            Set<Service<T>> candidates = this.serviceMap(domain).entrySet().stream().unordered()
                    .filter(entry -> Objects.equals(identifier, entry.getKey().serviceName()))
                    .map(Map.Entry::getValue).collect(Collectors.toUnmodifiableSet());
            if (candidates.isEmpty()) throw new NoSuchElementException("No service found for reference: " + identifier + " (" + domain.getTypeClass().getName() + ")");
            if (candidates.size() > 1) throw new NoSuchElementException("Ambiguous service reference: " + identifier + " (" + domain.getTypeClass().getName() + ")");
            return candidates.iterator().next();
        }
    }

    public <T extends Nameable> Service<T> get(Crocus.ServiceDomain<T> domain, Module module, String name) {
        Service<T> service = null;
        if (module.isNamed()) service = this.get(domain, new Key(module.getName(), name));
        if (service != null && service.module() == module) return service;
        String moduleName = Objects.requireNonNullElse(module.getName(), "ALL-UNNAMED");
        throw new NoSuchElementException("Service " + moduleName + ":" + name + " (" + domain.getTypeClass().getName() + ") not found.");
    }

    public <T extends Nameable> Service<T> get(Crocus.ServiceDomain<T> domain, Key key) {
        Service<T> service = this.serviceMap(domain).get(key);
        if (service == null) throw new NoSuchElementException("Service " + key + " not found.");
        return service;
    }

    public <T extends Nameable> Service<T> get(Crocus.ServiceDomain<T> domain, Class<? extends T> implementationType) {
        Set<Service<T>> candidates = this.serviceMap(domain).entrySet().stream().unordered()
                .map(Map.Entry::getValue)
                .filter(service -> service.implementationType() == implementationType)
                .collect(Collectors.toUnmodifiableSet());
        if (candidates.isEmpty()) throw new NoSuchElementException("No service found for implementation: " + implementationType + " (" + domain.getTypeClass().getName() + ")");
        if (candidates.size() > 1) throw new NoSuchElementException("Ambiguous service implementation: " + implementationType + " (" + domain.getTypeClass().getName() + ")");
        return candidates.iterator().next();
    }

    @SuppressWarnings("unchecked")
    private <T extends Nameable> Map<Key, Service<T>> serviceMap(Crocus.ServiceDomain<T> domain) {
        synchronized (this.lock) {
            if (this.services.containsKey(domain)) {
                return (Map<Key, Service<T>>) (Map<Key, ?>) this.services.get(domain);
            }
            Map<Key, Service<T>> serviceMap = loadServiceMap(this.layer, domain.getTypeClass());
            this.services.put(domain, (Map<Key, Service<?>>) (Map<Key, ?>) serviceMap);
            return serviceMap;
        }
    }

    private static <T extends Nameable> Map<Key, Service<T>> loadServiceMap(@Nullable ModuleLayer layer, Class<T> serviceClass) {
        if (layer == null) return Map.of();
        Map<Key, Service<T>> serviceMap = new HashMap<>();
        for (ServiceLoader.Provider<T> provider : ServiceLoader.load(layer, serviceClass).stream().toList()) {
            Module module = provider.type().getModule();
            if (!module.isNamed()) throw new ServiceConfigurationError("Crocus services can only be loaded from named modules.");
            T instance = provider.get();
            Key key = new Key(module.getName(), instance.name());
            Service<T> service = new Service<>(key, module, provider.type(), instance);
            if (serviceMap.put(key, service) != null) {
                throw new ServiceConfigurationError("Multiple service implementations for key " + key + " on " + serviceClass.getName());
            }
        }
        return Map.copyOf(serviceMap);
    }

    public record Key(String moduleName, String serviceName) {

        @Override
        public String toString() {
            return this.moduleName() + ":" + this.serviceName();
        }
    }

    public record Service<T extends Nameable>(Key key, Module module, Class<? extends T> implementationType, T instance) {

        @Override
        public String toString() {
        String moduleName = Objects.requireNonNullElse(this.module().getName(), "ALL-UNNAMED");
            return this.key() + "[" + moduleName + "/" + this.instance().getClass().getName() +"]";
        }
    }
}
