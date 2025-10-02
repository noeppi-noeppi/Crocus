package eu.tuxtown.crocus.jaxbnullity;

import com.sun.codemodel.JMethod;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CAttributePropertyInfo;
import com.sun.tools.xjc.model.CElementPropertyInfo;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.xml.sax.ErrorHandler;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class JaxbNullityPlugin extends Plugin {

    @Override
    public String getOptionName() {
        return "Xcrocus-nullity";
    }

    @Override
    public String getUsage() {
        return "  -" + this.getOptionName() + " add nullity annotations to generated code.";
    }

    @Override
    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) {
        for (ClassOutline modelClass : outline.getClasses()) {
            Set<String> nullableFields = new HashSet<>();
            Set<String> notNullFields = new HashSet<>();
            for (FieldOutline modelField : modelClass.getDeclaredFields()) {
                boolean primitive = modelField.getRawType().isPrimitive();
                boolean requiredElement = modelField.getPropertyInfo() instanceof CElementPropertyInfo ei && ei.isRequired();
                boolean requiredAttribute = modelField.getPropertyInfo() instanceof CAttributePropertyInfo ei && ei.isRequired();
                boolean collection = modelField.getPropertyInfo().isCollection();
                boolean notNull = collection || requiredElement || requiredAttribute;
                if (!primitive) {
                    if (notNull) {
                        notNullFields.add(modelField.getPropertyInfo().getName(true).toLowerCase(Locale.ROOT));
                    } else {
                        nullableFields.add(modelField.getPropertyInfo().getName(true).toLowerCase(Locale.ROOT));
                    }
                }
            }
            for (JMethod method : modelClass.getImplClass().methods()) {
                if (method.name().startsWith("get") && hasGetterSignature(method)) {
                    if (nullableFields.contains(method.name().substring(3).toLowerCase(Locale.ROOT))) {
                        method.annotate(Nullable.class);
                    } else if (notNullFields.contains(method.name().substring(3).toLowerCase(Locale.ROOT))) {
                        method.annotate(NotNull.class);
                    } else if (!method.type().isPrimitive()) {
                        method.annotate(UnknownNullability.class);
                    }
                }
                if (method.name().startsWith("set") && hasSetterSignature(method)) {
                    if (nullableFields.contains(method.name().substring(3).toLowerCase(Locale.ROOT))) {
                        method.listParams()[0].annotate(Nullable.class);
                    } else if (notNullFields.contains(method.name().substring(3).toLowerCase(Locale.ROOT))) {
                        method.listParams()[0].annotate(NotNull.class);
                    } else if (!method.listParamTypes()[0].isPrimitive()) {
                        method.listParams()[0].annotate(UnknownNullability.class);
                    }
                }
            }
        }
        return true;
    }

    private static boolean hasGetterSignature(JMethod method) {
        return method.listParamTypes().length == 0 && method.listVarParamType() == null && !"void".equals(method.type().binaryName());
    }
    
    private static boolean hasSetterSignature(JMethod method) {
        return method.listParamTypes().length == 1 && method.listVarParamType() == null && "void".equals(method.type().binaryName());
    }
}
