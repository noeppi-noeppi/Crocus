# The Google Plugin

The google plugin enabled Crocus to interface with various Google services.
Its plugin id is `tuxtown.crocus.google`.

## Attributes provided by the google plugin

- `google_timezone_override`: Specifies a timezone.
  If present, that timezone will be used for the event instead of the calendars default timezone.
- `google_blocks_time`: Sets whether the event blocks time on the google calendar (default: `false`).

## The `google` calendar type

The `google` calendar type allows pushing events into a Google calendar.
It has the following properties:

- `calendarId`: The id of the google calendar to access.
  Exactly one of `calendarId` and `birthdayCalendarId` must be set.
- `birthdayCalendarId`: The id of a personal google calendar.
  This instructs Crocus to access the birthday calendar associated with that personal calendar.
  Exactly one of `calendarId` and `birthdayCalendarId` must be set.
- `auth`: An OAuth client secret to access the Google services.

### A note on birthday calendars

Every personal calendar has an associated birthday calendars.
Group calendars don't have birthday calendars.
Due to restriction on Google birthday calendars, birthday calendars can only include annually repeating events.
Therefore, any event that is pushed into a birthday calendar automatically gets an annual repetition.

## The `people` event source

The `people` event source allows to load peoples birthdays from your Google contacts.
It has the following properties:

- `format`: Takes a unary operator on Strings that takes a persons name and returns an event name.
  Alternatively, you can provide a format string such as `Birthday of %s`.
- `auth`: An OAuth client secret to access the Google services.
