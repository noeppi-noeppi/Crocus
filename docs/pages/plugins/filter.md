# The Filter Plugin

The filter plugin defines some basic event filters.
Its plugin id is `tuxtown.crocus.filter`.

## The `simple` event filter

This event filter provides simple filtering logic based on filter rules.
In the filter config, multiple filter rules can be given.
For an event to be kept, all filter rules must match.
Available filter rules are:

- `when`: Takes a `Closure` that configures another `simple` filter.
  Only events that match the *when*-filter are considered for further filtering, events that don't match the *when*
   filter are always kept.
- `name`: Takes a string predicate that tests against the name of the event.
- `description`: Takes a string predicate that tests against the description of the event.
- `location`: Takes a string predicate that tests against the location of the event.
- `url`: Takes a `URI` predicate that tests against the url of the event.
- `before`: Takes a timestamp and only keeps events before that timestamp.
- `after`: Takes a timestamp and only keeps events after that timestamp.
- `during`: Takes two local dates and only keeps events between these.
  Both dates are inclusive in the date range.
  If multiple *during* clauses are given, they are ORed.
- `outside`: Takes two local dates and only keeps events not between these.
  Both dates are inclusive in the date range.
- `timezone`: A timezone that is used to compare all-day events against timestamps.
  If none is set, the system timezone is used.
- `attribute`: Takes an attribute and a predicate on the attributes value.
  The predicate must match on the attribute value for an event to be kept.
  If the event does not have the attribute, the predicate must match on `null`.
- `event`: Takes a predicate on the entire event object and matches on that.

Example:
```groovy
after '2024-11-30'
name { !it.contains('test') }
attribute('ical_url') { it != null }
```

## The `modify` event filter

This event filter allows to modify event properties.
In the filter config, multiple event modification rules can be given.
Available rules are:

- `when`: Takes a `Closure` that configures a `simple` filter.
  Only events that match the *when*-filter are considered for modification.
- `name`: Takes a unary operator that operates on event names.
- `description`: Takes a unary operator that operates on event descriptions.
- `location`: Takes a unary operator that operates on event locations.
- `url`: Takes a unary operator that operates on event urls.

Example:
```groovy
when {
    attribute('ical_status') { it == 'TENTATIVE' }
}
name { '(tentative) ' + it }
```
