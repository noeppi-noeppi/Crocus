# The Anniversary Plugin

The anniversary plugin allows to define annually repeating events in a flexible DSL.
Its plugin id is `tuxtown.crocus.anniversary`.

## The `anniversary` event source

The anniversary event source generates annually repeating events.

It has the following generic properties:

- `since` is a local date that marks the first day of the time period in which anniversary events will be generated.
- `until` is a local date that marks the last day of the time period in which anniversary events will be generated.
  It defaults to the end of the year that follows the current year.

Individual anniversaries can be created by the `day` function.
It takes a name of the anniversary, then optionally a description and an anniversary date definition.
The description can also be a function derived from the actual date on which the event takes place.
The anniversary date definition can be given as a closure.
Examples include:

```groovy
day('Anniversary 1', /* anniversary date definition */)
day('Anniversary 2', 'some description', /* anniversary date definition */)
day('Anniversary 3', { 'it is currently ' + it.month }, /* anniversary date definition */)
day('Anniversary 4') {
    /* anniversary date definition */
}
day('Anniversary 5', 'some description') {
    /* anniversary date definition */
}
```

### Anniversary date definitions

An anniversary date definition selects a subset of days from all days that fall between the configured `since` and
 `until` values.
There are three ways to create an anniversary date definition:

+-----------------------------------+----------------------------------------------------------------------------------+
| **Function**                      | **Description**                                                                  |
+===================================+==================================================================================+
| <span style="white-space:nowrap"> | Yields an anniversary date definition that selects every single day in the       |
| `always()`</span>                 | considered period of time.                                                       |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Yields an anniversary date definition that evaluates the provided closure for    |
| `only{ ... }`</span>              | every day in the considered period of time and select the dates for which the    |
|                                   | closure returns `true`.                                                          |
|                                   | For example `only { it.dayOfMonth + it.monthValue == 7 }` selects all dates,     |
|                                   | where the *month of day* plus the *month number* equal seven.                    |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Yields an anniversary date definition based on a filter.                         |
| `on(...)`</span> or               | See [below](#anniversary-date-filters).                                          |
| <span style="white-space:nowrap"> | Using `on(...)` is equivalent to `always().on(...)`.                             |
| `in(...)`</span>                  |                                                                                  |
+-----------------------------------+----------------------------------------------------------------------------------+

### Anniversary date modifiers

There are a variety of modifier methods that can be called on an anniversary date definition.

+-----------------------------------+----------------------------------------------------------------------------------+
| **Function**                      | **Description**                                                                  |
+===================================+==================================================================================+
| <span style="white-space:nowrap"> | Yields a date definition that selects all dates selected by both `dateDef1` and  |
| `dateDef1 & dateDef2`</span>      | `dateDef2`.                                                                      |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Yields a date definition that selects all dates selected by at least one of      |
| `dateDef1 | dateDef2`</span>      | `dateDef1` and `dateDef2`.                                                       |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Shifts all dates selected by `dateDef` by the given temporal amount into the     |
| `dateDef + temporalAmount`</span> | future.                                                                          |
|                                   | The temporal amount can be given like `'2 years + 1 week'`.                      |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Shifts all dates selected by `dateDef` by the given temporal amount into the     |
| `dateDef - temporalAmount`</span> | past.                                                                            |
|                                   | This works analogous to `dateDef + temporalAmount`.                              |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Adjusts all dates selected by `dateDef` to point to the first day of the month   |
| `dateDef.firstDayOfMonth()`       | in which the original selected date lies.                                        |
| </span>                           |                                                                                  |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Adjusts all dates selected by `dateDef` to point to the last day of the month in |
| `dateDef.lastDayOfMonth()`</span> | which the original selected date lies.                                           |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Adjusts all dates selected by `dateDef` to point to the first day of the year in |
| `dateDef.firstDayOfYear()`</span> | which the original selected date lies.                                           |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Adjusts all dates selected by `dateDef` to point to the last day of the year in  |
| `dateDef.lastDayOfYear()`</span>  | which the original selected date lies.                                           |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Adjusts all dates selected by `dateDef` to point to the first day of the year    |
| `dateDef.firstDayOfNextYear()`    | that follows the year in which the original selected date lies.                  |
| </span>                           | This is equivalent to `dateDef.lastDayOfYear() + '1 day'`                        |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Adjusts all dates selected by `dateDef` at least zero and at most six days into  |
| `dateDef.next(dow)`</span>        | the future, such that the newly selected day falls on the given day of week.     |
|                                   | A day of week can be given as a string containing its full name (`thursday`) or  |
|                                   | its three-letter abbreviation (`thu`).                                           |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Adjusts all dates selected by `dateDef` at least zero and at most six days into  |
| `dateDef.last(dow)`</span>        | the past, such that the newly selected day falls on the given day of week.       |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Adjusts all dates selected by `dateDef` at least one and at most seven days into |
| `dateDef.next(dow)`</span>        | the future, such that the newly selected day falls on the given day of week.     |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Adjusts all dates selected by `dateDef` at least one and at most seven days into |
| `dateDef.previous(dow)`</span>    | the past, such that the newly selected day falls on the given day of week.       |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | This is equivalent to `dateDef.firstDayOfMonth().next(dow)`.                     |
| `dateDef.firstInMonth(dow)`       |                                                                                  |
| </span>                           |                                                                                  |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | This is equivalent to `dateDef.lastDayOfMonth().last(dow)`.                      |
| `dateDef.lastInMonth(dow)`</span> |                                                                                  |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Applies an anniversary date filter to `dateDef`.                                 |
| `dateDef.on(...)`</span> or       | The result is a new anniversary date definition that considers all days selected |
| <span style="white-space:nowrap"> | by `dateDef` and only retains the days, that match the provided filter.          |
| `dateDef.in(...)`</span>          | See [below](#anniversary-date-filters).                                          |
+-----------------------------------+----------------------------------------------------------------------------------+

### Anniversary date filters

Anniversary date filters restrict the selected events over a domain.
A filter is always applied using the `on` or `in` function.
Both functions behave exactly the same.
All possible arguments to these functions are described in the table below:

+-----------------------------------+----------------------------------------------------------------------------------+
| **Filter**                        | **Description**                                                                  |
+===================================+==================================================================================+
| <span style="white-space:nowrap"> | Selects days based on the *month* and *day of month*.                            |
| `on(day, month)`</span>           | The *day of month* can be given as an integer (1-31), the *month* can be given   |
|                                   | as a string (`'september'` or `'sep'`).                                          |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Selects on a monthly basis based on the *day of month*.                          |
| `on(day)`</span>                  | The *day of month* must be given as an integer (1-31).                           |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Selects all days in the provided *month*.                                        |
| `on(month)`</span>                | The *month* can be given as a string (`'september'` or `'sep'`).                 |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Select all days that fall on the provided *day of the week*.                     |
| `on(dow)`</span>                  | The *day of the week* can be given as a string (`'thursday'` or `'thu'`).        |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Selects all days that fall in the provided *calendar week*.                      |
| `on(calendarWeek)`</span>         | This uses *ISO 8601* calendar week, where the first calendar week in a year is   |
|                                   | the week that contains the first thursday of that year.                          |
|                                   | The *calendar week* can be given as a string of the form `'cw##'` where `##`     |
|                                   | shall be replaced by the calendar week number.                                   |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Selects easter sundays in the gregorian calendar.                                |
| `on('easter')`</span>             |                                                                                  |
+-----------------------------------+----------------------------------------------------------------------------------+

### Examples

This section provides a few examples for anniversary configurations.

```groovy
day('Today is a day') {
    always()
}

day('New years eve') {
    on(31, 'december')
}

day('This anniversary will never occur') {
    on(31, 'february')
}

day('Friday the 13th', 'an unlucky day') {
    on(13) & on('friday') // or equivalent: on(13).on('friday')
}

day('Pentecost') {
    on('easter') + '49 days'
}

day('Mothers Day in the US') {
    on(1, 'may').next('sunday') + '1 week'
}

day('1. Advent Sunday') {
    on(25, 'december').previous('sunday') - '3 weeks'
}

day('calendar week 42', 'this will be a week in mid-october') {
    on('cw42')
}
```
