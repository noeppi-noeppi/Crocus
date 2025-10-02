package eu.tuxtown.crocus.core.dsl;

import eu.tuxtown.crocus.api.Crocus;
import eu.tuxtown.crocus.api.service.Nameable;
import eu.tuxtown.crocus.core.dsl.meta.DslTypeTransformation;
import eu.tuxtown.crocus.core.dsl.meta.InterceptingGroovyObjectSupport;
import eu.tuxtown.crocus.core.loader.Services;
import groovy.lang.Closure;
import groovy.lang.MissingMethodException;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Objects;

@NotNullByDefault
public abstract class ServiceContainer<T extends Nameable> extends InterceptingGroovyObjectSupport {

    private final Services services;
    private final Crocus.ServiceDomain<T> domain;

    protected ServiceContainer(Services services, Crocus.ServiceDomain<T> domain) {
        this.services = services;
        this.domain = domain;
    }

    protected abstract Object addService(Services.Service<T> service, String name, Closure<?> config);

    @Override
    protected @Nullable Object invokeMethod(String type, Object[] args) {
        Services.Service<T> service = null;
        int offset = 0;
        if (args.length == 2) {
            try {
                service = this.services.resolve(this.domain, type);
            } catch (NoSuchElementException e) {
                //
            }
        } else if (args.length == 3 && Objects.equals(type, "add")) {
            String serviceType = (String) DslTypeTransformation.castToType(args[0], String.class);
            if (serviceType == null) {
                throw new MissingMethodException(type, this.getClass(), args);
            }
            service = this.services.resolve(this.domain, serviceType);
            offset = 1;
        }

        if (service != null) {
            String serviceName = (String) DslTypeTransformation.castToType(args[offset], String.class);
            Closure<?> config = (Closure<?>) DslTypeTransformation.castToType(args[offset + 1], Closure.class);
            if (serviceName == null || config == null) {
                throw new MissingMethodException(type, this.getClass(), args);
            }
            return this.addService(service, serviceName, config);
        } else {
            return this.delegateUpwards(type, args);
        }
    }
}
