package eu.tuxtown.crocus.core.dsl.meta.metaclass;

import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.core.dsl.meta.member.HandleMetaMethod;
import groovy.lang.ExpandoMetaClass;
import groovy.lang.MetaClassRegistry;
import groovy.lang.MetaMethod;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAmount;

public class EventBuilderMetaClass extends ExpandoMetaClass {

    public EventBuilderMetaClass(MetaClassRegistry reg, Class<? extends Event.Builder> cls) {
        super(reg, cls, false, false, new MetaMethod[0]);
        try {
            this.registerInstanceMethod(new HandleMetaMethod(
                    Event.Builder.class, Modifier.PUBLIC, "timed",
                    MethodHandles.lookup().findStatic(EventBuilderMetaClass.class, "timed", MethodType.methodType(Event.Builder.class, Event.Builder.class, ZonedDateTime.class, TemporalAmount.class))
            ));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Failed to setup MetaClass for " + cls.getName(), e);
        }
    }

    private static Event.Builder timed(Event.Builder self, ZonedDateTime start, TemporalAmount amount) {
        return self.time(start.toInstant(), start.plus(amount).toInstant());
    }
}
