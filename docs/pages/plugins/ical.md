# The iCalendar Plugin

The iCalendar plugin allows loading events from iCal calendars.
Its plugin id is `tuxtown.crocus.ical`.

## Attributes provided by the iCalendar plugin

- `ical_priority`: The value of the `PRIORITY` property or `0` if not present.
- `ical_status`: The value of the `STATUS` property if present.
- `ical_color`: The value of the `COLOR` property if present.
- `ical_classification`: The value of the `CLASSIFICATION` property if present.

## The `ìcal` event source

The ical event source loads events from an iCalendar resource.
As iCalendar supports recurring events and Crocus does not, recurring events are converted to a series of non-recurring
 events.
It has the following properties:

- `source` is a `Resource` from where the iCal calendar shall be loaded.
- `timezone` is the default time zone if the iCalendar does not define time zone information.
  It defaults to the system timezone.
- `charset` defines the character encoding in which the XML schedule is encoded.
  The default is UTF-8.
- `repeatFor` is a *temporal amount* (such as `'2 years'`) that defines, for how long event repetitions should be
   created, measured from the start of the first event occurrence.
  Crocus calculated two timespans for event repetition, one using `repeatFor` and another one using `repeatFromNow`.
  The longer timespan will be used to calculate event repetitions.
- `repeatFromNow` is a *temporal amount* (such as `'2 years'`) that defines, for how long event repetitions should at
   least be created, measured from now on.
  Crocus calculated two timespans for event repetition, one using `repeatFor` and another one using `repeatFromNow`.
  The longer timespan will be used to calculate event repetitions.
- `sequences` defines how to treat `SEQUENCE` numbers in iCal events.
  See below for further information.

### Sequence behaviour

Many iCal implementations implement the behaviour of `SEQUENCE` number in a different way the iCalendar standard
 mandates.
Therefore, Crocus provides four strategies for treating `SEQUENCE` numbers:

- `uniform`:
  Requires all events in an event group to have the same sequence number.
  If that is not the case, the Crocus synchronization fails.
  This fails for some iCalendar data that is valid according to the standard.
  However, it helps to detect issues with iCal implementations that don't follow the standard.
  This is the default behaviour in Crocus.
- `obsolete`:
  Higher sequence numbers obsolete lower sequence numbers.
  Sequence numbers are treated globally on a whole event group.
  This is the behaviour according to the iCalendar standard.
- `isolate`:
  Higher sequence numbers obsolete lower sequence numbers.
  Sequence numbers are treated locally per repetition instance.
- `coexist`:
  Sequence numbers are ignored entirely.
  Every event is used, even if multiple events mark the same repetition instance.
