import bootstrap.spi.Entrypoint;
import eu.tuxtown.crocus.api.service.AttributeProvider;
import eu.tuxtown.crocus.api.service.CalendarType;
import eu.tuxtown.crocus.api.service.EventFilterType;
import eu.tuxtown.crocus.api.service.EventSourceType;
import eu.tuxtown.crocus.builtin.DumpSourceType;
import eu.tuxtown.crocus.core.Main;

// core may not specify transitive requires or plugin loading may fail.
module tuxtown.crocus.core {
    requires org.jetbrains.annotations;

    requires java.net.http;
    requires bootstrap.spi;
    requires bootstrap.jar;
    requires joptsimple;
    requires org.apache.groovy;
    requires com.google.gson;

    exports eu.tuxtown.crocus.api;
    exports eu.tuxtown.crocus.api.attribute;
    exports eu.tuxtown.crocus.api.calendar;
    exports eu.tuxtown.crocus.api.delegate;
    exports eu.tuxtown.crocus.api.resource;
    exports eu.tuxtown.crocus.api.service;

    uses AttributeProvider;
    uses CalendarType;
    uses EventFilterType;
    uses EventSourceType;

    provides Entrypoint with Main;
    provides EventSourceType with DumpSourceType;
}
