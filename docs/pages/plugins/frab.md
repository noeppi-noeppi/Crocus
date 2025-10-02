# The Frab Plugin

The frab plugin allows loading conference schedules from [Frab](https://frab.github.io/frab/)-compatible
 [XML schedule](https://github.com/frab/schedule.xml) files.
Its plugin id is `tuxtown.crocus.frab`.

## Attributes provided by the frab plugin

- `frab_room`: A string containing the conference room in which the event takes place.
- `frab_type`: The type string associated with a frab event.
- `frab_people`: A list of people holding a frab event.

## The `frab` event source

The `frab` event source allows loading events from a Frab-compatible XML schedule.
It has the following properties:

- `source` is a `Resource` from where the XML schedule shall be loaded.
- `timezone` is the default time zone if the schedule does not define timestamps and does not set a local timezone.
  It defaults to the system timezone.
- `charset` defines the character encoding in which the XML schedule is encoded.
  The default is UTF-8.
