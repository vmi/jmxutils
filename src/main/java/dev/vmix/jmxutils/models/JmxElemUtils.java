/*
 *
 */
package dev.vmix.jmxutils.models;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;

@SuppressWarnings("javadoc")
public final class JmxElemUtils {

    private JmxElemUtils() {
    }

    public static JmxMap newEmptyMap(String name, String valueTypeName, String description) {
        return new JmxMap(name, valueTypeName, description);
    }

    private static boolean hasKeyValuePairOnly(CompositeType cType) {
        String typeName = cType.getTypeName();
        Set<String> keySet = cType.keySet();
        return typeName.startsWith("Map<java.lang.String,")
            && keySet.size() == 2
            && keySet.contains("key")
            && keySet.contains("value");
    }

    public static JmxElem<?> wrap(String name, Object value, String valueTypeName, String description) {
        if (value == null) {
            return new JmxEntity<>(name, null, valueTypeName, description);
        }
        if (valueTypeName == null) {
            valueTypeName = value.getClass().getCanonicalName();
        }
        if (value instanceof Map) {
            return new JmxMap(name, (Map<?, ?>) value, valueTypeName, description);
        } else if (value instanceof List) {
            return new JmxList(name, (List<?>) value, valueTypeName, description);
        } else if (value instanceof Object[]) {
            JmxList list = new JmxList(name, valueTypeName, description);
            for (Object item : (Object[]) value) {
                list.add(item, null, null);
            }
            return list;
        } else if (value instanceof CompositeData) {
            CompositeData cData = (CompositeData) value;
            CompositeType cType = cData.getCompositeType();
            String cDesc = cType.getDescription();
            if (cDesc != null && (cDesc.equals(name) || cDesc.equals(cType.getTypeName()))) {
                cDesc = null;
            }
            if (hasKeyValuePairOnly(cType)) {
                String key = (String) cData.get("key");
                Object iValue = cData.get("value");
                OpenType<?> iType = cType.getType("value");
                String iTypeName = iType.getTypeName();
                String iDescription = iType.getDescription();
                if (iDescription != null && (iDescription.equals(key) || iDescription.equals(iTypeName))) {
                    iDescription = null;
                }
                return wrap(key, iValue, iTypeName, iDescription);
            } else {
                JmxMap map = new JmxMap(name, cType.getTypeName(), cDesc);
                cType.keySet().stream().sorted().forEach(key -> {
                    Object iValue = cData.get(key);
                    OpenType<?> iType = cType.getType(key);
                    String iTypeName = iType.getTypeName();
                    String iDescription = iType.getDescription();
                    if (iDescription != null && (iDescription.equals(key) || iDescription.equals(iTypeName))) {
                        iDescription = null;
                    }
                    map.put(key, iValue, iTypeName, iDescription);
                });
                return map;
            }
        } else {
            return new JmxEntity<>(name, value, valueTypeName, description);
        }
    }
}
