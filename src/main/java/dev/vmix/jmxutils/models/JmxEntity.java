/*
 *
 */
package dev.vmix.jmxutils.models;

@SuppressWarnings("javadoc")
public class JmxEntity<T> extends AbstractJmxElem<T> {

    JmxEntity(String name, T value, String typeName, String description) {
        super(name, value, typeName, description);
    }

    @Override
    public ElemType getElemType() {
        return ElemType.ENTITY;
    }

    @Override
    public boolean walk(Walker walker, String... parentKeys) {
        throw new UnsupportedOperationException();
    }
}
