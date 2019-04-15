package dev.vmix.jmxutils;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import dev.vmix.jmxutils.models.JmxMap;
import dev.vmix.jmxutils.models.JmxElemUtils;

/**
 * JMX Client.
 */
public class JmxClient implements Closeable {

    private static final String NL = System.lineSeparator();

    private final JMXConnector connector;
    private final MBeanServerConnection connection;

    /**
     * Constructor.
     *
     * @param host host.
     * @param port port.
     */
    public JmxClient(String host, int port) {
        String url = String.format("service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi", host, port);
        try {
            JMXServiceURL target = new JMXServiceURL(url);
            connector = JMXConnectorFactory.connect(target);
            connection = connector.getMBeanServerConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get list of MBean.
     *
     * @return list of MBean.
     */
    public List<ObjectInstance> getMBeans() {
        try {
            Set<ObjectInstance> mBeans = connection.queryMBeans(null, null);
            return mBeans.stream().sorted((a, b) -> a.getObjectName().compareTo(b.getObjectName()))
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get MBean information.
     *
     * @param objectNameStr object name.
     * @return MBean information.
     */
    public JmxMap getMBeanInfo(String objectNameStr) {
        try {
            ObjectName objectName = new ObjectName(objectNameStr);
            MBeanInfo info = connection.getMBeanInfo(objectName);
            JmxMap map = JmxElemUtils.newEmptyMap(null, info.getClassName(), info.getDescription());
            MBeanAttributeInfo[] attrInfos = info.getAttributes();
            String[] attrNames = Arrays.stream(attrInfos).map(attrInfo -> attrInfo.getName()).toArray(String[]::new);
            AttributeList attrList = connection.getAttributes(objectName, attrNames);
            for (int aIndex = 0; aIndex < attrInfos.length; aIndex++) {
                MBeanAttributeInfo attrInfo = attrInfos[aIndex];
                Attribute attr = (Attribute) attrList.get(aIndex);
                String name = attrInfo.getName();
                String type = attrInfo.getType();
                String desc = attrInfo.getDescription();
                Object value = attr.getValue();
                map.put(name, value, type, desc);
            }
            return map;
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(e);
        } catch (InstanceNotFoundException | IntrospectionException | ReflectionException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        connector.close();
    }
}
