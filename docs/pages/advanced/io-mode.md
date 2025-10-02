# IO Mode

Usually, when you invoke Crocus, it performs an incremental synchronization of all calendars in the system
 configuration.
However, there is also another mode of operation called IO mode.
In this mode, Crocus performs the following steps:

- Aggregate events from a list of event sources.
- Apply a list of event filters on these events.
- Push the events into a list of calendars.

The sources, filters and sinks are given on the command line.
Note that in IO mode, all sinks will always receive the exact same events.
To push different events to different calendars, multiple runs are required.

IO mode is activated by using the `--io` command line option.
In IO mode, the available command line options differ.
A table of available options can be found below.

+-----------------------------------+----------------------------------------------------------------------------------+
| **Option**                        | **Description**                                                                  |
+===================================+==================================================================================+
| <span style="white-space:nowrap"> | Reads the given *file* as json file in Crocus own event json format when         |
| `--load <file>`</span>            | aggregating events.                                                              |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Dump all events into the given *file* in Crocus event json format.               |
| `--dump <file>`</span>            |                                                                                  |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Aggregates all events retrieved from the event source of the given *name* in the |
| `--source <name>`</span>          | system configuration.                                                            |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Aggregates all events that would have been placed in the calendar of the given   |
| `--calendar <name>`</span>        | *name* in the system configuration.                                              |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Pushes all events into the calendar of the given *name* in the system            |
| `--sink <name>`</span>            | configuration. This option causes a non-incremental synchronization.             |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Pushes all events into the calendar of the given *name* in the system            |
| `--isink <name>`</span>           | configuration. This option causes an incremental synchronization.                |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Adds a filter to the list of filters to run on all aggregated events. The format |
| `--filter <filter>`</span>        | such a *filter specification* is explained below.                                |
+-----------------------------------+----------------------------------------------------------------------------------+

## Filter Specifications

The `--filter` option requires a filter specification as its argument.
Filter specifications have the following general format `id=code`, where `id` is a service identifier as explained in
the [configuration reference](./configuration-format.md) and `code` is some groovy code to configure that filter.
Filter specifications can also declare a *namespace*.
In this case, the format is `id@ns=code`.
The namespace `ns` limits which events are subject to filtering.
A namespaced filter will only filter events that originated from an event source with a name equal to the filter's
 namespace.
Sources added with the `--load` option have no name and therefore use `file` as their namespace.

The following list contains some examples of valid filter specifications

- `modify=name { it += " (filtered)" }` uses the *modify* filter from the [Filter Plugin](../plugins/filter.md) to
   append the string `(filtered)` to the name of events.
- `tuxtown.crocus.filter:simple@file=after "2024-01-01"` uses the *simple* filter from the
   [Filter Plugin](../plugins/filter.md) to filter out events before the start of 2024.
  This filter specification uses a qualified service identifier to select the filter type.
- `simple=` another example using the *simple* filter from the [Filter Plugin](../plugins/filter.md).
  In this case, none of the configuration methods of the filter are invoked.
  Therefore, this filter effectively is a no-operation.

Note that filter definitions often contains special characters that need to be quoted in a shell.

## Examples

Here are some example of IO mode.

- Synchronise only a single calendar from the system configuration (in this case `mycalendar`).
  ```shell
  crocus --io --calendar mycalendar --isink mycalendar
  ```
- Read events from `in.json` and remove all events in August 2021, then write the result to `out.json`.
  ```shell
  crocus --io --load in.json --filter 'simple=outside("2021-08-01","2021-08-31")' --dump out.json
  ```
- Clear a calendar from the system configuration.
  As there are no event sources defined, the list of aggregated events is empty, which will clear the calendar.
  ```shell
  crocus --io --sink mycalendar
  ```
- Perform an event dump for a single event source without touching any calendars.
  ```shell
  crocus --io --source mysource --dump event-dump/mysource.json
  ```
