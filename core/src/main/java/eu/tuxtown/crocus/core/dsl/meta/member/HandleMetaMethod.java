package eu.tuxtown.crocus.core.dsl.meta.member;

import groovy.lang.MetaMethod;
import org.codehaus.groovy.reflection.CachedClass;
import org.codehaus.groovy.reflection.ReflectionCache;
import org.jetbrains.annotations.NotNullByDefault;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;

@NotNullByDefault
public class HandleMetaMethod extends MetaMethod {

    private final CachedClass declaringClass;
    private final int modifiers;
    private final String name;
    private final CallSite callSite;

    public HandleMetaMethod(Class<?> declaringClass, int modifiers, String name, MethodHandle handle) {
        this(declaringClass, modifiers, name, new ConstantCallSite(handle));
    }

    public HandleMetaMethod(Class<?> declaringClass, int modifiers, String name, CallSite callSite) {
        super(parameterArray(modifiers, callSite.type()));
        this.declaringClass = ReflectionCache.getCachedClass(declaringClass);
        this.modifiers = modifiers;
        this.name = name;
        this.callSite = callSite;
    }

    private static Class<?>[] parameterArray(int modifiers, MethodType type) {
        if ((modifiers & Modifier.STATIC) == 0 && type.parameterCount() != 0) {
            return type.dropParameterTypes(0, 1).parameterArray();
        } else {
            return type.parameterArray();
        }
    }

    @Override
    public int getModifiers() {
        return this.modifiers;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<?> getReturnType() {
        return this.callSite.type().returnType();
    }

    @Override
    public boolean isCacheable() {
        return this.callSite instanceof ConstantCallSite;
    }

    @Override
    public CachedClass getDeclaringClass() {
        return this.declaringClass;
    }

    @Override
    public Object invoke(Object object, Object[] arguments) {
        MethodHandle target = this.callSite.getTarget();
        if ((this.modifiers & Modifier.STATIC) == 0) {
            target = target.bindTo(object);
        }
        try {
            return target.invokeWithArguments(arguments);
        } catch (Throwable e) {
            doThrow(e);
            throw new Error(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void doThrow(Throwable t) throws T {
        throw (T) t;
    }
}
