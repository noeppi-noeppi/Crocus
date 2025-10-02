package eu.tuxtown.crocus.core.dsl.meta.metaclass;

import eu.tuxtown.crocus.core.dsl.Globals;
import eu.tuxtown.crocus.core.dsl.meta.DslTypeTransformation;
import groovy.lang.*;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@NotNullByDefault
public class DslMetaClass extends DelegatingMetaClass {

    private DslMetaClass(MetaClass parent) {
        super(parent);
    }

    public static void applyTo(Object object) {
        if (object instanceof Class<?>) return;
        MetaClass metaClass = DefaultGroovyMethods.getMetaClass(object);
        if (metaClass instanceof DslMetaClass) return;
        DslMetaClass newMetaClass = new DslMetaClass(metaClass);
        newMetaClass.initialize();
        DefaultGroovyMethods.setMetaClass(object, newMetaClass);
    }

    public static void applyToClass(MetaClassRegistry reg, Class<?> cls) {
        MetaClass metaClass = reg.getMetaClass(cls);
        DslMetaClass newMetaClass = new DslMetaClass(metaClass);
        newMetaClass.initialize();
        reg.setMetaClass(cls, newMetaClass);
    }

    @Override
    public Object getProperty(Object object, String property) {
        if (Objects.equals(property, "globals")) {
            return new Globals();
        } else {
            return super.getProperty(object, property);
        }
    }

    @Override
    public void setProperty(Object object, String property, Object newValue) {
        MetaProperty metaProperty = this.getMetaProperty(property);
        if (metaProperty != null) {
            metaProperty.setProperty(object, DslTypeTransformation.castToType(newValue, metaProperty.getType()));
        } else {
            super.setProperty(object, property, newValue);
        }
    }

    @Override
    public @Nullable Object invokeMethod(Object object, String methodName, @Nullable Object arg) {
        Object[] args = switch (arg) {
            case null -> new Object[0];
            case Tuple<?> tuple -> tuple.toArray();
            case Object[] array -> array;
            default -> new Object[]{arg};
        };
        return this.invokeMethod(object, methodName, args);
    }

    @Override
    public @Nullable Object invokeMethod(Object object, String methodName, @Nullable Object[] arguments) {
        if (Objects.equals(methodName, "globals") && arguments.length == 0) {
            return new Globals();
        }
        MetaMethod metaMethod = null;
        for (MetaMethod m : this.getMethods()) {
            if (methodName.equals(m.getName())) {
                int argNum = m.getParameterTypes().length;
                if (arguments.length == argNum || (m.isVargsMethod() && arguments.length >= argNum - 1)) {
                    if (metaMethod != null) return super.invokeMethod(object, methodName, arguments);
                    metaMethod = m;
                }
            }
        }
        if (metaMethod != null) {
            @Nullable Object[] castArguments = DslTypeTransformation.castArguments(arguments, metaMethod.getParameterTypes());
            if (castArguments != null) {
                return metaMethod.invoke(object, castArguments);
            }
        }
        return super.invokeMethod(object, methodName, arguments);
    }
}
