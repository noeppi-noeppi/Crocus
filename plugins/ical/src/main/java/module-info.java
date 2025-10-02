import eu.tuxtown.crocus.api.service.AttributeProvider;
import eu.tuxtown.crocus.api.service.EventSourceType;
import eu.tuxtown.crocus.ical.ICalSourceType;
import eu.tuxtown.crocus.ical.api.ICalAttributes;

module tuxtown.crocus.ical {
    requires static org.jetbrains.annotations;

    requires tuxtown.crocus.core;
    requires tuxtown.crocus.sesquiannual;

    exports eu.tuxtown.crocus.ical.api;

    provides AttributeProvider with ICalAttributes;
    provides EventSourceType with ICalSourceType;
}
