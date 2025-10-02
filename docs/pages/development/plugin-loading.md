# Plugin Loading

This page explains the plugin loading process and gives an introduction on how to write custom plugins.

When Crocus starts, it loads the bundled plugins documented under in the [Plugins section](../plugins/core.md) as well
 as all plugins from the `plugins` directory in the Crocus working directory.
A plugin is a regular *Jar*-file that is a named, non-automatic module (i.e. it provides a `module-info.class`).
The module name becomes the plugin identifier.
Plugins can bundle additional dependencies, see the [plugin bundling](./bundling.md) page for details.

Plugins can extend Crocus by providing services using the regular java service-loader mechanism.
The relevant service interfaces are:

- `eu.tuxtown.crocus.api.service.CalendarType` for [calendar types](#calendar-types)
- `eu.tuxtown.crocus.api.service.EventSourceType` for [event source types](#event-source-types)
- `eu.tuxtown.crocus.api.service.EventFilterType` for [event filter types](#event-filter-types)
- `eu.tuxtown.crocus.api.service.AttributeProvider` for [attribute providers](#attribute-providers)

Each of these service provider interfaces is explained in a relevant section below.

## Calendar Types

Calendar types are responsible for creating `Calendar` objects using the [delegate configuration](./configuration.md)
 mechanism.
A calendar type must have a name that is unique among all calendar types provided by the same plugin.

## Event Source Types

Event source types are responsible for creating `EventSource` objects using the
 [delegate configuration](./configuration.md) mechanism.
An event source type must have a name that is unique among all event source types provided by the same plugin.

## Event Filter Types

Event filter types are responsible for creating `EventFilter` objects using the
 [delegate configuration](./configuration.md) mechanism.
An event filter type must have a name that is unique among all event filter types provided by the same plugin.

## Attribute Providers

Attribute providers define a single `registerAttributes` method that receives an attribute registry.
It can then register attributes that have been created using the static `Attribute.create` methods.
Attribute names must be unique among all loaded plugins.
