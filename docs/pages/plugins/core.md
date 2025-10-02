# The Crocus Core Plugin

This part of the documentation covers all builtin plugins bundled with Crocus.
The Crocus core acts as a plugin itself.
It is described on this page.
The following pages cover the actual builtin plugins.

The plugin id of the Crocus core is `tuxtown.crocus.core`.

## The `dump` event source

The `dump` event source allows loading events from a Crocus event dump as generated with the
 [`--dump`](../getting-started/run.md) option.
It has the following properties:

- `source` is a `Resource` from where the event dump shall be loaded.
- `charset` defines the character encoding in which the event dump is encoded.
