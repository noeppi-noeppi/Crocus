package eu.tuxtown.crocus.core.dsl.meta.metaclass;

import eu.tuxtown.crocus.api.calendar.Event;
import eu.tuxtown.crocus.core.dsl.meta.member.HandleMetaMethod;
import groovy.lang.ExpandoMetaClass;
import groovy.lang.MetaClassRegistry;
import groovy.lang.MetaMethod;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class EventTimeMetaClass extends ExpandoMetaClass {

    public EventTimeMetaClass(MetaClassRegistry reg, Class<? extends Event.EventTime> cls) {
        super(reg, cls, false, false, mixin());
    }

    private static MetaMethod[] mixin() {
        try {
            return new MetaMethod[]{
                    new HandleMetaMethod(
                            Event.EventTime.class, Modifier.PUBLIC, "start",
                            MethodHandles.lookup().findStatic(EventTimeMetaClass.class, "start", MethodType.methodType(Instant.class, Event.EventTime.class))
                    ),
                    new HandleMetaMethod(
                            Event.EventTime.class, Modifier.PUBLIC, "end",
                            MethodHandles.lookup().findStatic(EventTimeMetaClass.class, "end", MethodType.methodType(Instant.class, Event.EventTime.class))
                    ),
                    new HandleMetaMethod(
                            Event.EventTime.class, Modifier.PUBLIC, "startDay",
                            MethodHandles.lookup().findStatic(EventTimeMetaClass.class, "startDay", MethodType.methodType(LocalDate.class, Event.EventTime.class))
                    ),
                    new HandleMetaMethod(
                            Event.EventTime.class, Modifier.PUBLIC, "endDay",
                            MethodHandles.lookup().findStatic(EventTimeMetaClass.class, "endDay", MethodType.methodType(LocalDate.class, Event.EventTime.class))
                    )
            };
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Failed to setup MetaClass for " + Event.EventTime.class.getName(), e);
        }
    }

    private static Instant start(Event.EventTime time) {
        return time.start(ZoneId.systemDefault());
    }

    private static Instant end(Event.EventTime time) {
        return time.end(ZoneId.systemDefault());
    }

    private static LocalDate startDay(Event.EventTime time) {
        return time.startDay(ZoneId.systemDefault());
    }

    private static LocalDate endDay(Event.EventTime time) {
        return time.endDay(ZoneId.systemDefault());
    }
}
