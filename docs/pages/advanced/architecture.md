# Crocus Architecture

Crocus uses a plugin-based architecture.
There is the *Crocus Core*, which provides an interface for *Plugins*.
A plugin can provide one or multiple *Services* which extends Crocus' functionality.

There are four different kinds of services that plugins can provide:

- *Calendar Types* provide a certain type of calendar (event sink).
  Calendars are responsible for synchronizing events to some kind of external sink.
  An example for a calendar type is *Google Calendar*  provided by the [Google Plugin](../plugins/google.md).
- *Event Source Types* provide a certain type of event source.
  Event sources are responsible for fetching events from some kind of external source.
  An example for an event source type is *iCalendar* provided by the [iCalendar Plugin](../plugins/ical.md).
- *Event Filter Types* provide a certain type of event filter.
  Event filters are responsible for filtering and modifying events after they have been retrieved from an event source.
  An example for an event filter type are *Simple Filters* provided by the [Filter Plugin](../plugins/filter.md).
- *Attribute Providers* are used to register additional [attributes](#attributes) to store additional data on events.

## Events

The central type object that Crocus deals with is the event.
Crocus models an event in a rather simple way, using only these attributes:

- An identifier that must be unique among all events from the same event source.
- A name of the event
- An optional description of the event
- An optional location of the event
- An optional absolute URL describing the event
- Date and Time information

Most event sources provide additional information about events, and most calendars accept additional information about
 events.
Crocus intentionally keeps the number of event fields to a minimum, but allows plugins to register additional
 [attributes](#attributes) that can attach extra information to events.

Unlike many other calendar-related applications, Crocus events do not support recurrence.
Recurring events are probably the feature that causes the most complexity and incompatibility issues in calendar-related
 software.

Therefore, Crocus does not support recurrence at all.
Event sources that fetch events from a source supporting recurrence must expand these into individual event instances
 and pass them to Crocus separately.
Calendars that push events to a sink supporting recurrence may detect the expanded recurrence and collapse them back
 into a single event.
Consequently, Crocus cannot fully support events repeating infinitely and must cut repetition off at some point in time.

The date and time of an event is always one of the following types:

- A start date and an end date, each consisting of a year, month and day.
  Both the start and end days are part of the event.
- A start timestamp and an end timestamp, both with nanosecond precision.
- A start timestamp with nanosecond precision but no end timestamp.
  In this case, the event is treated as open-ended with an unknown duration.

Note that an event does not store time zone information in any of these cases.
Time zones are a property of calendars, not events.
Therefore, calendar implementations are required to convert timestamps into the appropriate local time for their event
 sink.

### Attributes

Attributes allow plugins to attach extra information to events.
Plugins can register attributes by providing an *attribute provider* service.

An attribute consists of a unique name and an optional default value, as well as a function that encodes how to
 serialise and deserialise its values.
The Crocus core itself provides no additional attributes.
However, some of the bundled plugins do. See the [plugin section](../plugins/core.md) for details.

### Filters

Event filters are added to event sources.
As well as discarding events, an event filter can modify them as they pass through the filter.
Multiple filters added to an event source are invoked in the order they were added.

Event filters cannot modify the unique identifiers of events passing through them and therefore cannot turn a single
 event into multiple events.

## Service Interface

This section documents, what logic event sources, filters and calendars need to provide and what logic is performed by
 the Crocus core.

### Event Source

Event sources only have a single function, which is fetching the underlying source and returning a list of events for
 further processing by crocus.
Crocus always expects an event source to return all events from the underlying source.
However, if the underlying source support incremental requests, where it only sends changes that were made since the
 last run, the event source service may store the retrieved events in the `plugin-data` folder and then use incremental
 fetch to fetch changes and build a complete event list from these.

The only function of event sources is to fetch the underlying source and return a list of events for further processing
 by Crocus.
Crocus always expects an event source to return all events from the underlying source.
However, if the underlying source supports incremental requests and only sends changes made since the last run, the
 event source service can make of that and store the retrieved events in the `plugin-data` folder.
On the next run, it can then use incremental fetch to retrieve the changes and compile a full list of events.

### Event Filters

Event filters also only have a single function.
They retrieve one event after another.
For each event, they may return it unchanged, return a changed event return no event at all, in which case the event is
 dropped.
Filters should not keep internal state as it is not specified, in which order they will receive the events retrieved
 from an event source.

### Calendars

As Crocus pushes incremental updates to calendars by default, the API that calendars need to provide is more complex
 than that of event sources and filters.
Calendar implementations need to provide the following functionality:

- *Synchronise changed events*:
  The calendar receives two lists: one of events that are either new or modified, and one of deleted events.
  It then needs to apply these changes to the downstream event sink.
- *Delete some events only if they exist*:
  In the event of an error or crash, Crocus stores a list of event identifiers that could exist in the calendar but are
   not in a known state.
  The next time Crocus runs, the calendar needs to delete all these events to return the calendar to a known state.
- *Delete all events in the calendar*:
  This is mainly required to support the `--no-incremental` option.
  In this case, Crocus has no knowledge of the calendar's state and resets it completely before pushing any events.

Downstream calendars often use their own identifiers for events that are randomly generated by the calendar itself.
Crocus handles the mapping from internal Crocus event identifiers to identifiers native to the downstream calendar.
Therefore, calendar implementations must notify Crocus immediately once a new native event identifier has been assigned
 to an event.
