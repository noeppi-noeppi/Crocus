# The CSV Plugin

The CSV plugin allows assembling events from CSV files.
Its plugin id is `tuxtown.crocus.csv`.

## The `cs` event source

The CSV event source loads events from a CSV resource.
It has the following properties:

- `source` is a `Resource` from which the CSV file will be loaded.
- `charset` defines the character encoding in which the CSV file is encoded.
  The default is UTF-8.
- `separator` is the CSV separator string.
  The default is a comma.
- `comment` is an optional comment character that introduces line comments in the read CSV file.
- `events`: Takes a `Closure` that assembles events from CSVV records.
  The `Closure` receives the record as argument and can set event properties based on that.
  See below for a list of event properties that can be set.

## Event properties

Inside the `events` block, the following properties may be set:

- `name`: Sets the event name. This property is mandatory.
- `description`, `location`, `url`: Sets the corresponding value on the event.
- `time(<start>)`: Makes the event an open-end event that starts on the provided date.
- `time(<start>, <end>)`: Makes the event a timed event with the provided start and end dates and times.
- `timed(<start>, <duration>)`: Makes the event a timed event with the provided start date and time and the provided 
   duration.
- `day(<start>, <end>)`: Makes the event an all-day timed event with the provided start and end dates.
  Both start and end date are inclusive.
- `attribute(<attribute>, <value>)`: Sets a non-standard attribute on the event.

## Example

Consider the following CSV file:
```csv
name,start
Event 1,2014-11-21T17:10:00+02:00
Event 2,2006-01-02T15:04:05+07:00
```

A matching CSV source could look like this.
Every event will have a duration of 2 hours and 15 minutes.
```groovy
csv('example') {
  configure {
    source 'example.csv'
    events {
      name it.name
      timed(it.start, '2 hours + 15 minutes')
    }
  }
}
```
