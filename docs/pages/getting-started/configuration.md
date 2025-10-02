# Configuring Crocus

The Crocus configuration file is actually a [Groovy](https://groovy-lang.org/) script.
That means, you have the full power and flexibility of Groovy available.
For example, it's easily possible to filter events based on a line of custom Groovy code directly embedded in the
 configuration file.

Crocus reads its main configuration from a file named `config.groovy`.
An example configuration is provided below:

```groovy title="config.groovy"
calendars {
    google('mycalendar') { // (1)!
        calendarId 'your_google_calendar_id@group.calendar.google.com' // (2)!
        auth secrets.google  // (3)!
    }
}

sources {
    ical('debian-releases') { // (4)!
        into 'mycalendar' // (5)!
        configure { // (6)!
            source 'https://release.debian.org/release-calendar.ics' // (7)!
        }
        filter('simple') { // (8)!
            after '2024-01-01' // (9)!
        }
    }
}
```

1. Create a calendar (event sink) named `mycalendar`, that is backed by a Google calendar. The following block contains
   configuration specific to Google calendar.
2. Define the calendar id of the Google calendar to access.
3. Define the secrets required to access the Google calendar. This makes use of the [secret loading](./secrets.md)
   mechanism. See the [Google Plugin](../plugins/google.md) for information on the required secrets.
4. Create an event source named `debian-releases` that retrieves events by accessing a calendar in *iCalendar* format.
5. Specify, that events obtained from the `debian-releases` event source shall be placed in the calendar named
   `mycalendar`.
6. Configure the event source `debian-releases`. The following block contains configuration specific to iCalendar
   sources.
7. Specify the URL of the iCalendar resource.
8. Add a filter of type `simple` to the event source `debian-releases`. The following block contains configuration
   specific to filters of type `simple`.
9. Discard events that happen entirely before the start of 2024. As the provided temporal information has no associated
   timezone, the system timezone will be used to convert it to an instant.

The above configuration creates a single calendar (event sink) named `mycalendar` that is backed by a Google calendar.
The calendar uses the [Secrets](./secrets.md) mechanism to load the secrets required to access the calendar.
Additionally, a single event source named `debian-releases` is defined, which queries the Debian Release feed provided
 in iCalendar format.
Events obtained from this event source are filtered according to the filter that has been defined on the event source,
 discarding events before 2024.
After filtering, these events are pushed into `mycalendar`

To test the configuration file, enter the directory in which it resides and run the `crocus` command with no arguments.
Further information on the accepted command line arguments and the directory structure that Crocus adheres to can be
 found on the [Running Crocus](./run.md) page.

The configuration format is explained in detail on the [configuration reference](../advanced/configuration-format.md)
 page.
