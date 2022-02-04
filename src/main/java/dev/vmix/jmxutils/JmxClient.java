package dev.vmix.jmxutils;

import java.io.Closeable;
import java.io.IOException;
import java.rmi.UnmarshalException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import dev.vmix.jmxutils.models.JmxElemUtils;
import dev.vmix.jmxutils.models.JmxMap;

/**
 * JMX Client.
 */
public class JmxClient implements Closeable {

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
            for (MBeanAttributeInfo attrInfo : attrInfos) {
                if (!attrInfo.isReadable()) {
                    continue;
                }
                String attrName = attrInfo.getName();
                Object attrValue;
                String attrType = attrInfo.getType();
                String attrDesc = attrInfo.getDescription();
                if (attrDesc != null && (attrDesc.equals(attrName) || attrDesc.equals(attrType))) {
                    attrDesc = null;
                }
                try {
                    attrValue = connection.getAttribute(objectName, attrName);
                } catch (AttributeNotFoundException | MBeanException | UnmarshalException e) {
                    attrValue = String.format("%s: %s", e.getClass().getCanonicalName(), e.getMessage());
                }
                map.put(attrName, attrValue, attrType, attrDesc);
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
