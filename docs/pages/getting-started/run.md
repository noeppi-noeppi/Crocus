# Running Crocus

Crocus can be run by calling the `crocus` start script bundled with the distribution.
If invoked without any arguments, Crocus reads the configuration in the current working directory and performs an
 incremental synchronization of all calendars listed in the configuration file.
The behaviour can be changed by passing some of the supported options:

+-----------------------------------+----------------------------------------------------------------------------------+
| **Option**                        | **Description**                                                                  |
+===================================+==================================================================================+
| <span style="white-space:nowrap"> | Shows a short summary of all available options.                                  |
| `--help`</span>                   |                                                                                  |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Sets the working directory, where Crocus looks for the configuration file and    |
| `--path <path>`</span>            | stores its data.                                                                 |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Produces more verbose output.                                                    |
| `--verbose`</span>                |                                                                                  |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Activates *IO Mode*, a completely different mode of operation, described on a    |
| `--io`</span>                     | [separate page](../advanced/io-mode.md).                                         |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Writes the events of each queried event source to a json file before pushing the |
| `--dump`</span>                   | events into the calendars.                                                       |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Do not attempt to perform an incremental synchronization. Instead, delete all    |
| `--no-incremental`</span>         | events from each calendar before adding new ones. This is mainly useful, if you  |
|                                   | lost the saved state from the previous run.                                      |
+-----------------------------------+----------------------------------------------------------------------------------+

## Working Directory Structure

This section gives an overview of the files and directories Crocus creates or expects in its working directory.

+-----------------------------------+----------------------------------------------------------------------------------+
| **Path**                          | **Description**                                                                  |
+===================================+==================================================================================+
| <span style="white-space:nowrap"> | The user-provided main configuration file. See                                   |
| `config.groovy`</span>            | [Configuring Crocus](./configuration.md) for details.                            |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Directory for additional plugins that are loaded at runtime.                     |
| `plugins/`</span>                 |                                                                                  |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | [properties file](https://en.wikipedia.org/wiki/.properties) that is read by the |
| `secrets.properties`</span>       | [secret loading](./secrets.md) mechanism if it exists.                           |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Directory, whose contents are read by the [secret loading](./secrets.md)         |
| `secrets/`</span>                 | mechanism if it exists.                                                          |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Contains the last known state of all calendars defined in the system             |
| `calendars/`</span>               | configuration in a format internal to Crocus. These files are required for       |
|                                   | incremental synchronization.                                                     |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Directory where event dumps are written to if the `--dump` command line option   |
| `event-dump/`</span>              | is given.                                                                        |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Contains data directories for plugins. The data stored here depends on the       |
| `plugin-data/`</span>             | plugin.                                                                          |
+-----------------------------------+----------------------------------------------------------------------------------+
| <span style="white-space:nowrap"> | Contains data directories for plugins. This directory generally stores sensitive |
| `secret-data/`</span>             | data that should be kept secret. For example, session and refresh tokens may be  |
|                                   | stored here.                                                                     |
+-----------------------------------+----------------------------------------------------------------------------------+
