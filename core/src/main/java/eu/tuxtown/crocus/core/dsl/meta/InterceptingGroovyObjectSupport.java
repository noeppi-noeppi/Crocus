package eu.tuxtown.crocus.core.dsl.meta;

import eu.tuxtown.crocus.core.dsl.Globals;
import groovy.lang.GroovyInterceptable;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import groovy.lang.Tuple;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@NotNullByDefault
public abstract class InterceptingGroovyObjectSupport extends GroovyObjectSupport implements GroovyInterceptable {

    @Override
    public Object getProperty(String propertyName) {
        if (Objects.equals(propertyName, "globals")) {
            return new Globals();
        } else {
            return super.getProperty(propertyName);
        }
    }

    @Override
    public @Nullable Object invokeMethod(String name, @Nullable Object arg) {
        Object[] args = switch (arg) {
            case null -> new Object[0];
            case Tuple<?> tuple -> tuple.toArray();
            case Object[] array -> array;
            default -> new Object[]{arg};
        };
        if (Objects.equals(name, "globals") && args.length == 0) {
            return new Globals();
        }
        return this.invokeMethod(name, args);
    }

    protected @Nullable Object invokeMethod(String name, Object[] args) {
        throw new MissingMethodException(name, this.getClass(), args);
    }

    protected @Nullable Object delegateUpwards(String name, Object[] args) {
        return super.invokeMethod(name, args);
    }
}
