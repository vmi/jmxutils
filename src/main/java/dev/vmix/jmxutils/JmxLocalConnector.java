package dev.vmix.jmxutils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

public class JmxLocalConnector {

    private static final String VIRTUAL_MACHINE_CLASS = "com.sun.tools.attach.VirtualMachine";
    private static final String LOCAL_CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

    public static String getLocalConnectorAddress(int pid) {
        return getLocalConnectorAddress(Integer.toString(pid));
    }

    private static void addToolsClassPath() {
        String javaHome = System.getProperty("java.home");
        Path toolsPath = Paths.get(javaHome, "../lib/tools.jar");
        if (Files.notExists(toolsPath)) {
            // tools.jar does not found.
            return;
        }
        if (!(ClassLoader.getSystemClassLoader() instanceof URLClassLoader)) {
            // Cannot add tools.jar classpath dynamically.
            return;
        }
        try {
            URLClassLoader scl = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(scl, toolsPath.toUri().toURL());
        } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | MalformedURLException e) {
            // Cannot add tools.jar classpath dynamically.
            // Ignored.
        }
    }

    public static String getLocalConnectorAddress(String pidStr) {
        addToolsClassPath();
        try {
            Class.forName(VIRTUAL_MACHINE_CLASS);
        } catch (ClassNotFoundException e) {
            // Cannot load VirtualMachine class.
            return null;
        }
        return new VMWrapper(pidStr).getLocalConnectorAddress();
    }

    private static class VMWrapper {

        private final VirtualMachine vm;

        private VMWrapper(String pidStr) {
            try {
                vm = VirtualMachine.attach(pidStr);
            } catch (AttachNotSupportedException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        private String getLocalConnectorAddress() {
            try {
                String addr = vm.getAgentProperties().getProperty(LOCAL_CONNECTOR_ADDRESS);
                if (addr == null) {
                    String javaHome = System.getProperty("java.home");
                    Path agentPath = Paths.get(javaHome, "lib/management-agent.jar");
                    if (Files.exists(agentPath)) {
                        vm.loadAgent(agentPath.toString());
                    }
                    addr = vm.getAgentProperties().getProperty(LOCAL_CONNECTOR_ADDRESS);
                }
                return addr;
            } catch (IOException | AgentLoadException | AgentInitializationException e) {
                return null;
            }
        }
    }
}
