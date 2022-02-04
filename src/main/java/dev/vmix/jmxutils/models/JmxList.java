/*
 *
 */
package dev.vmix.jmxutils.models;

import java.util.ArrayList;
import java.util.List;

public class JmxList extends AbstractJmxElem<List<JmxElem<?>>> {

    JmxList(String name, String valueTypeName, String description) {
        super(name, new ArrayList<>(), valueTypeName, description);
    }

    JmxList(String name, List<?> list, String valueTypeName, String description) {
        this(name, valueTypeName, description);
        list.forEach(item -> add(item, null, null));
    }

    @Override
    public ElemType getElemType() {
        return ElemType.LIST;
    }

    public void add(Object item, String itemTypeName, String description) {
        String index = Integer.toString(value.size());
        JmxElem<?> elem = JmxElemUtils.wrap(index, item, itemTypeName, description);
        value.add(elem);
    }

    @Override
    public boolean walk(Walker walker, String... parentKeys) {
        int index = -1;
        int lastIndex = value.size() - 1;
        for (JmxElem<?> item : value) {
            ++index;
            if (!walker.callback(walker, item, index == 0, index == lastIndex, getElemType(), parentKeys)) {
                return false;
            }
        }
        return true;
    }
}
