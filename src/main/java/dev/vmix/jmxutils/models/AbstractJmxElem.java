/*
 *
 */
package dev.vmix.jmxutils.models;

import java.util.Objects;

@SuppressWarnings("javadoc")
public abstract class AbstractJmxElem<T> implements JmxElem<T> {

    final String name;
    final T value;
    final String valueTypeName;
    final String description;

    public AbstractJmxElem(String name, T value, String valueTypeName, String description) {
        this.name = name;
        this.value = value;
        this.valueTypeName = valueTypeName;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public String getValueTypeName() {
        return valueTypeName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append('[')
            .append(name)
            .append(" = ")
            .append(Objects.toString(value));
        if (valueTypeName != null) {
            buf.append(": ").append(valueTypeName);
        }
        if (description != null) {
            buf.append(" (").append(valueTypeName).append(')');
        }
        buf.append(']');
        return buf.toString();
    }
}
