/*
 *
 */
package dev.vmix.jmxutils.models;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings("javadoc")
public class JmxMap extends AbstractJmxElem<Map<String, JmxElem<?>>> {

    JmxMap(String name, String valueTypeName, String description) {
        super(name, new LinkedHashMap<>(), valueTypeName, description);
    }

    JmxMap(String name, Map<?, ?> map, String valueTypeName, String description) {
        this(name, valueTypeName, description);
        map.forEach((key, value) -> put(key.toString(), value, null, null));
    }

    @Override
    public ElemType getElemType() {
        return ElemType.MAP;
    }

    public JmxElem<?> get(String key) {
        return value.get(key);
    }

    public void put(String key, Object value, String typeName, String description) {
        JmxElem<?> elem = JmxElemUtils.wrap(key, value, typeName, description);
        this.value.put(key, elem);
    }

    @Override
    public boolean walk(Walker walker, String... parentKeys) {
        int index = -1;
        int lastIndex = value.size() - 1;
        for (Entry<String, JmxElem<?>> entry : value.entrySet()) {
            ++index;
            if (!walker.callback(walker, entry.getValue(), index == 0, index == lastIndex, getElemType(), parentKeys)) {
                return false;
            }
        }
        return true;
    }
}
