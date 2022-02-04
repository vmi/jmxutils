/*
 *
 */
package dev.vmix.jmxutils.models;

import java.util.Arrays;

public interface JmxElem<T> {

    public enum ElemType {
        ENTITY, LIST, MAP
    }

    @FunctionalInterface
    public interface Walker {

        boolean callback(Walker walker, JmxElem<?> elem, boolean isFirst, boolean isLast, ElemType parentElemType, String... parentKeys);
    }

    ElemType getElemType();

    String getName();

    T getValue();

    String getValueTypeName();

    String getDescription();

    boolean walk(Walker walker, String... parentKeys);

    default boolean walk(Walker walker, String[] parentKeys, String currentKey) {
        int len = parentKeys.length;
        String[] newKeys = Arrays.copyOf(parentKeys, len + 1);
        newKeys[len] = currentKey;
        return walk(walker, newKeys);
    }
}
