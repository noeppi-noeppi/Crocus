package eu.tuxtown.crocus.core.dsl;

import eu.tuxtown.crocus.api.resource.Resource;
import eu.tuxtown.crocus.core.dsl.meta.DslTypeTransformation;
import eu.tuxtown.crocus.core.dsl.meta.metaclass.DslMetaClass;
import eu.tuxtown.crocus.impl.resource.HttpResource;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.Tuple;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

@NotNullByDefault
public class Globals extends GroovyObjectSupport {

    public Instant getSystemTime() {
        return Instant.now();
    }

    public ZoneId getSystemTimezone() {
        return ZoneId.systemDefault();
    }

    public Resource http(URI uri) {
        return HttpResource.builder(uri).build();
    }

    public Resource http(URI uri, @DelegatesTo(value = HttpResource.Builder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
        HttpResource.Builder builder = HttpResource.builder(uri);
        DslMetaClass.applyTo(builder);
        closure.setDelegate(builder);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
        return builder.build();
    }

    @Override
    public @Nullable Object invokeMethod(String methodName, @Nullable Object arg) {
        Object[] args = switch (arg) {
            case null -> new Object[0];
            case Tuple<?> tuple -> tuple.toArray();
            case Object[] array -> array;
            default -> new Object[]{arg};
        };
        return this.invokeMethod(methodName, args);
    }

    private @Nullable Object invokeMethod(String methodName, Object[] arguments) {
        if (Objects.equals("http", methodName) && arguments.length == 1 || arguments.length == 2) {
            Object[] newArguments = new Object[arguments.length];
            System.arraycopy(arguments, 0, newArguments, 0, arguments.length);
            Object uri = DslTypeTransformation.castToType(newArguments[0], URI.class);
            if (uri != null) newArguments[0] = uri;
            return super.invokeMethod("http", newArguments);
        } else {
            return super.invokeMethod(methodName, arguments);
        }
    }
}
