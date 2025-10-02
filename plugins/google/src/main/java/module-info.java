import eu.tuxtown.crocus.api.service.AttributeProvider;
import eu.tuxtown.crocus.api.service.CalendarType;
import eu.tuxtown.crocus.api.service.EventSourceType;
import eu.tuxtown.crocus.google.api.GoogleAttributes;
import eu.tuxtown.crocus.google.calendar.GoogleCalendarType;
import eu.tuxtown.crocus.google.people.PeopleSourceType;

module tuxtown.crocus.google {
    requires static org.jetbrains.annotations;

    requires tuxtown.crocus.core;
    requires google.api.client;
    requires com.google.api.client;
    requires com.google.api.client.auth;
    requires com.google.api.client.json.gson;
    requires com.google.api.services.calendar;
    requires com.google.api.services.people;
    requires com.google.api.client.extensions.jetty.auth;
    requires com.google.api.client.extensions.java6.auth;

    exports eu.tuxtown.crocus.google.api;

    provides AttributeProvider with GoogleAttributes;
    provides CalendarType with GoogleCalendarType;
    provides EventSourceType with PeopleSourceType;
}
