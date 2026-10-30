# The CalDAV Plugin

The CalDAV plugin allows syncing events into a calendar that supports the CalDAV protocol.
Its plugin id is `tuxtown.crocus.caldav`.
This plugin requires the [ical plugin](./ical.md) to be present.

## The `caldav` calendar type

The `caldav` calendar type allows pushing events into a CalDAV calendar.
It has the following properties:

- `uri`: The URI at which the CalDAV calender can be accessed.
- `authenticate`: Provide a username and a password to authenticate with the CalDAV server.
