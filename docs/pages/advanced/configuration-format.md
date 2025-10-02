# Configuration Reference

This page provides a detailed explanation of the system configuration file, `config.groovy`.
It is an ordinary Groovy script that is executed in a specific context to generate a set of calendars and event sources.
This means that all regular Groovy features can be used in the configuration file.
However, in most cases, it won't be necessary to use any complex Groovy features, and the file resembles an ordinary,
 expressive configuration file.
Nevertheless, you have the full power of Groovy at your disposal when required.

## Service identifiers

A *service identifier* is a string that identifies a service type, such as a *calendar type*, *event source type*, or
 *event filter type*.
Each service type has a unique name inside the plugin that provides the service.

A service identifier can take one of two forms:

- An *unqualified service identifier* is simply the name of a service.
  This name must be unique across all loaded plugins.
- A *qualified service identifier* consists of a plugin name followed by a colon (`:`) and a service name.
  This type of service identifier always uniquely references a service.

For example, the `simple` filter from the [filter plugin](../plugins/filter.md) can be referenced as
 `tuxtown.crocus.filter:simple` or, if no other event filter of name `simple` exists in another plugin, just as
 `simple`.

## The `calendars` block

The `calendars` block defines calendars that Crocus can synchronize events into.
Calendars can be added like this:
```groovy
calendars {
    add('tuxtown.crocus.google:google', 'mycalendar') {
        // Calendar configuration
    }
}
```
The first argument of the `add` function is a [service identifier](#service-identifiers), the second argument is the
 calendars name.
The nested calendar configuration block is used for service specific configuration.
See the [Plugins section](../plugins/core.md) for details on the configuration options for different calendar types.

There is also a shorthand syntax.
If the calendar type can be expressed as an *unqualified service identifier*, a calendar can also be added like this:
```groovy
calendars {
    google('mycalendar') {
        // Calendar configuration
    }
}
```
In this case, the function name is the service identifier for the calendar type.

## The `sources` block

The `sources` block defines event sources that Crocus can fetch events from.
As with the `calendars` block, there are two options of adding an event source.
Calendars can be added like this:
```groovy
sources {
    add('tuxtown.crocus.ical:ical', 'mysource1') {
        // Event source configuration
    }
    ical('mysource2') {
        // Event source configuration
    }
}
```
The latter approach only works for *unqualified service identifiers*.

Contrary to calendars the event source specific configuration does not go directly into the nested configuration block.
Instead, the following functions are defined there:

- The `into` function takes the name of a calendar as argument and specifies that events from th event source shall be
   placed in that calendar.
  Example:
  ```groovy
  add('tuxtown.crocus.ical:ical', 'mysource') {
      into 'mycalendar'
  }
  ```
- The `configure` function takes a configuration block with the configuration specific to the event source type.
  See the [Plugins section](../plugins/core.md) for details on the configuration options for different event source
   types.
  Example:
  ```groovy
  add('tuxtown.crocus.ical:ical', 'mysource') {
      configure {
          // Configuration options specific to the type of event source
      }
  }
  ```
- The `filter` function takes an argument that is a service identifier of an event filter and adds that filter to the
   filter chain for this event source.
  Example:
  ```groovy
  add('tuxtown.crocus.ical:ical', 'mysource') {
      filter('tuxtown.crocus.filter:simple') {
          // Configuration options specific to the event filter
      }
  }
  ```

## Globals & Secrets

Crocus defines two more global symbols in the configuration file:
The `secrets` object provides access to secrets as explained in [Providing Secrets](../getting-started/secrets.md).
The `globals` object holds some global utility functions, namely:

- `globals.systemTime` is the current system time as a timestamp.
  It does not include timezone information.
- `globals.systemTimezone` is the system timezone.
- `globals.http` allows to configure an HTTP resource.
  It can be used like this:
  ```groovy
  globals.http("https://some.http/url") {
      // Additional configuration options
  }
  ```
  where the *additional configuration options are
  - `header(name, value)` sets a header of the HTTP connection.
  - `noRedirects()` disabled following of HTTP redirects.
  - `authenticate(host, username, password)` adds HTTP basic authentication for a specific host.
    This function can be called multiple times to set different authentication for different hosts.
    This is especially useful, if the requested URL redirects to another host.
  - `trust(certificate)` trusts a certificate, where certificate can be a `pem` or `der` file in `X.509` format.
    If at least one custom certificate is specified, system certificates are ignored.

All globals can also be called without the `globals`-prefix, however the `globals` prefix exists for cases in which a
 name conflicts with a variable or function declared in a lower scope.

## Automatic type conversations

Crocus provides a whole range of automatic type conversations.
That means, in most cases it's possible to write the value of a type such as `Path`, `URI` or `ZonedDateTime` as a
 string and let Crocus auto-convert it to the required type.
This greatly improves the readability of the configuration file.
For an overview over the exact type conversations performed, see the
 [Service Configuration](../development/configuration.md) page.

Crocus uses the following date format for converting strings into temporal values:
```
yyyy-MM-dd["T"HH:mm:ss[.nnnnnnnnn][OFFSET["["TZID"]"]]]
```
Brackets mark an optional part, text between quotes (`"`) is treated literally.
`OFFSET` is `+HH[:MM]`, `-HH[:MM]`, or `Z` for `UTC`.
`TZID` is a timezone identifier such as `Europe/London`.
If time zone information is required but not provided, the system time zone is used.
