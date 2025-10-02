package eu.tuxtown.crocus.core.dsl;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Properties;

@NotNullByDefault
public class DslSecrets extends GroovyObjectSupport {

    private final Properties properties;

    public DslSecrets(Properties properties) {
        this.properties = properties;
    }

    public String getAt(String propertyName) {
        if (this.properties.containsKey(propertyName)) {
            return this.properties.getProperty(propertyName);
        } else {
            throw new NoSuchElementException(propertyName);
        }
    }

    @Override
    public @Nullable Object getProperty(String propertyName) {
        if (this.properties.containsKey(propertyName)) {
            return this.properties.getProperty(propertyName);
        } else {
            throw new MissingPropertyException(propertyName, this.getClass());
        }
    }
}
