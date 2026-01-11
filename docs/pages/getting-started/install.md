# Installing Crocus

The latest Crocus release can be downloaded from [GitHub](https://github.com/noeppi-noeppi/Crocus/releases/latest).
It is shipped as a *zip* or *tar.gz* archive that can be unpacked at your preferred installation location.
Crocus can then be executed by calling the `bin/crocus` script.

Crocus requires at least Java 25.
For Crocus to find a Java installation, either the `JAVA_HOME` environment variable must point to the location of a Java
 installation or the `java` command must be found on the `PATH`.
On Unix systems, its is possible to configure the java installation used by Crocus in a file named `conf/default` in the
 extracted distribution like this:

```shell title="conf/default"
JAVA_HOME=/path/to/java/installation
```

## Building from Source

It is also possible to build Crocus from source:
```shell
git clone 'https://github.com/noeppi-noeppi/Crocus.git'
cd Crocus
./gradlew build
```
You'll need a suitable Java Development Kit such as
 [Eclipse Temurin](https://adoptium.net/de/temurin/releases?version=21) installed.
After Crocus has been built, you'll find the binary distributions in `core/build/distributions`
