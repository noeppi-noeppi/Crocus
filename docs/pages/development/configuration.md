# Service Configuration

Calendar types, event source types, and event filter types often have additional configuration settings.
The available configuration options are defined using the *delegate configuration* mechanism.

To create a new object, Crocus first invokes the `createDelegate()` method.
This method returns a *delegate object*.
Crocus then evaluates the configuration block (a groovy `Closure`) with this object as its delegate.
After that, the calendar, event source, or event filter is created by calling the `create()` method which receives the
 delegate object as its argument.

Delegate objects are usually mutable and define methods that correspond to configuration options.
A sample delegate class can is found below:
```java
public class SampleConfig {

    private Instant since = null; // (1)!

    public void since(Instant since) {
        this.since = since;
    }

    public Instant getSince() {
        if (this.since == null) {
            throw new NoSuchElementException("No start date set.");
        }
        return this.since;
    }
}
```

1. A default value for the configuration option can be set here.

This implements a single config property, named `since`, that takes a timestamp as an argument.
It can for example be used like this:
```groovy
since '2014-11-21T17:10:00+02' // (1)!
```

1. [https://www.youtube.com/watch?v=vcFBwt1nu2U&t=3124s](https://www.youtube.com/watch?v=vcFBwt1nu2U&t=3124s)

## Type Transformations

As explained in the [configuration reference](../advanced/configuration-format.md#automatic-type-conversations), Crocus
 automatically converts values to required types in most cases.
This section gives an overview over the available conversations.
This serves as a reference to plugin authors to know, which types they can simply use for configuration values.

- All regular groovy type transformations apply.
  This includes transformations from `Closure` to any functional interface.
- `Closure` is converted to `DelegateConfiguration`.
  This class provides an easy way to have nested configuration blocks with nested delegate objects.
- `String` is converted to any enum by case insensitive match with the enum value name.
- `String` is converted to `Attribute`.
  Attributes are found by their unique name.
- `String`, `Date`, and `TemporalAccessor` are converted to `Instant`, `ZonedDateTime`, `OffsetDateTime`,
   `LocalDateTime`, or `LocalDate`.
  If the resulting type has a time component and the input representation has not, the time component will be set to
   midnight.
  If the resulting type has a time zone and the input representation has not, the time zone will be set to the system
   time zone.
- `String` is converted to `ZoneId` by the time zone name.
- `String` is converted to `TemporalAmount`, `Period`, or `Duration`.
  The string must have a format like `12 days + 3 hous - 1 second`.
- `String` is converted to `DayOfWeek`.
  The string can use the three-letter abbreviation (eg. `tue`), or the full name of the day (eg. `thursday`).
- `String` is converted to `Month`.
  The string can use the three-letter abbreviation (eg. `sep`), or the full name of the day (eg. `september`).
- `String` is converted to `Path` or `File`.
  Relative path strings are resolved relative to the crocus working directory.
- `URL`, `URI`, `Path`, `File`, and `String` are converted to `URL`, or `URI`.
- `URL`, `URI`, `Path`, `File`, `String`, and `byte[]` are converted to `Resource`.
  A *resource* is something that can be read as an `InputStream`.
  Strings can only converted to resources if they are valid URIs or paths.
- `String` is converted to `Charset` by the charset name.
