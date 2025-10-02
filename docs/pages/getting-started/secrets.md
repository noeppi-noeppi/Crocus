# Providing Secrets

Crocus provides a mechanism to pass secrets to services like calendars and event sources.
When evaluating the system configuration, loaded secrets will be provided as properties on a global object named
 `secrets`.
That means a secret named `abc` can be accessed as `secrets.abc` in the system configuration.

Crocus uses two sources to load secrets:

- If a file named `secrets.properties` exists in the Crocus working directory, it is read as a
   [properties-file](https://en.wikipedia.org/wiki/.properties).
  Each key-value pair in the file is loaded as a secret.
  The key determines the secret's name and the value determines the secret's value.
- If a directory named `secrets` exists in the Crocus working directory, Crocus loads each regular file under that
   directory as a secret.
  The filename becomes the secret's name and the file contents become the secret's value.
