package eu.tuxtown.crocus.api.attribute;

import eu.tuxtown.crocus.api.service.AttributeProvider;
import eu.tuxtown.crocus.core.CrocusRuntime;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@NotNullByDefault
public class Attributes {

    /**
     * Creates a new non-standard attribute. The returned attribute must be registered before it can be used. See
     * {@link AttributeProvider}.
     */
    public static <T> Attribute<T> create(String name, Class<T> cls, AttributeAdapter<T> adapter) {
        Module module = cls.getModule();
        if (module.isNamed() && CrocusRuntime.get().pluginLayer().findModule(module.getName()).stream().anyMatch(m -> m == module)) {
            return new Impl<>(name, module.getName(), cls.getName(), adapter);
        } else {
            throw new IllegalArgumentException("The class used for an attribute must be loaded from the plugin layer.");
        }
    }

    /**
     * Creates a new non-standard attribute. The returned attribute must be registered before it can be used. See
     * {@link AttributeProvider}.
     */
    public static <T> DefaultedAttribute<T> create(String name, Class<T> cls, AttributeAdapter<T> adapter, T defaultValue) {
        Module module = cls.getModule();
        if (module.isNamed() && CrocusRuntime.get().pluginLayer().findModule(module.getName()).stream().anyMatch(m -> m == module)) {
            return new DefaultedImpl<>(name, module.getName(), cls.getName(), adapter, defaultValue);
        } else {
            throw new IllegalArgumentException("The class used for an attribute must be loaded from the plugin layer.");
        }
    }

    /**
     * Creates a new non-standard list attribute. The returned attribute must be registered before it can be used. See
     * {@link AttributeProvider}.
     */
    @SuppressWarnings("unchecked")
    public static <T> DefaultedAttribute<List<T>> createList(String name, Class<T> cls, AttributeAdapter<T> adapter) {
        return create(name, (Class<List<T>>) (Class<?>) List.class, AttributeAdapters.list(cls, adapter), List.of());
    }

    /**
     * Gets a non-standard attribute by name.
     */
    public static Optional<Attribute<?>> get(String name) {
        return Optional.ofNullable(CrocusRuntime.get().attributes().get(name));
    }

    /**
     * Gets a non-standard attribute by name and type. The given class must exactly match the class that was used
     * to define the attribute.
     */
    public static <T> Optional<Attribute<T>> get(String name, Class<T> cls) {
        Attribute<?> attr = CrocusRuntime.get().attributes().get(name);
        if (attr instanceof Impl<?> impl && Objects.equals(cls.getModule().getName(), impl.moduleName) && Objects.equals(cls.getName(), impl.className)) {
            //noinspection unchecked
            return Optional.of((Attribute<T>) attr);
        } else {
            return Optional.empty();
        }
    }

    static sealed class Impl<T> implements Attribute<T> permits DefaultedImpl {

        private final String name;
        private final String moduleName;
        private final String className;

        private final AttributeAdapter<T> adapter;

        private Impl(String name, String moduleName, String className, AttributeAdapter<T> adapter) {
            this.name = name;
            this.moduleName = moduleName;
            this.className = className;
            this.adapter = adapter;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public AttributeAdapter<T> adapter() {
            return this.adapter;
        }

        @Override
        public int hashCode() {
            return this.name().hashCode();
        }

        @Override
        public int compareTo(Attribute<?> obj) {
            if (obj instanceof Impl<?> impl) {
                int result = this.name.compareTo(impl.name);
                if (result != 0) return result;
                result = this.moduleName.compareTo(impl.moduleName);
                if (result != 0) return result;
                return this.className.compareTo(impl.className);
            } else {
                return 0;
            }
        }

        @Override
        public String toString() {
            return "Attribute(" + this.name + ")";
        }
    }

    static final class DefaultedImpl<T> extends Impl<T> implements DefaultedAttribute<T> {

        private final T defaultValue;

        private DefaultedImpl(String name, String moduleName, String className, AttributeAdapter<T> adapter, T defaultValue) {
            super(name, moduleName, className, adapter);
            this.defaultValue = Objects.requireNonNull(defaultValue);
        }

        @Override
        public T defaultValue() {
            return this.defaultValue;
        }
    }
}
