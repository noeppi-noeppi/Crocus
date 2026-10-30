package eu.tuxtown.crocus.core.dsl.util;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;

import java.util.Map;
import java.util.stream.Collectors;

public class DynamicObject extends GroovyObjectSupport {

    private final Map<String, ?> properties;

    public DynamicObject(Map<String, ?> properties) {
        this.properties = Map.copyOf(properties);
    }

    public Object getAt(String propertyName) {
        return this.getProperty(propertyName);
    }

    public boolean isCase(String propertyName) {
        return this.properties.containsKey(propertyName);
    }

    @Override
    public Object getProperty(String propertyName) {
        if (this.properties.containsKey(propertyName)) {
            return this.properties.get(propertyName);
        } else {
            throw new MissingPropertyException("Property " + propertyName + " not found in dynamic object.");
        }
    }

    @Override
    public void setProperty(String propertyName, Object newValue) {
        if (this.properties.containsKey(propertyName)) {
            throw new UnsupportedOperationException("Can't change property " + propertyName);
        } else {
            throw new MissingPropertyException("Property " + propertyName + " not found in dynamic object.");
        }
    }

    @Override
    public String toString() {
        return this.properties.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    return key + "=" + entry.getValue();
                })
                .collect(Collectors.joining(",", "{", "}"));
    }
}
