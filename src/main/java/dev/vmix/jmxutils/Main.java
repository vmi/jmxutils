package dev.vmix.jmxutils;

import java.io.IOException;
import java.io.PrintStream;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.management.ObjectName;

import static dev.vmix.jmxutils.CliHelper.*;

import dev.vmix.jmxutils.models.JmxElem.ElemType;
import dev.vmix.jmxutils.models.JmxMap;

/**
 * JMX Utility Main
 */
public class Main {

    private static final String[] HELP_MESSAGE = {
        "Usage: java -jar jmxutils.jar [-v] -h HOST -p PORT COMMAND ARGS ...",
        "       java -jar jmxutils.jar [-v] -P PID COMMAND ARGS ...",
        "",
        "[OPTIONS]",
        "-v      - verbose mode",
        "-h HOST - target host",
        "-p PORT - target port",
        "-P PID  - target PID",
        "",
        "[COMMAND]",
        "list [PATTERN] - list MBeans. (PATTERN is written as a regular expression)",
        "show NAME      - show MBean information."
    };

    private static final String USE_CODEBASE_ONLY = "java.rmi.server.useCodebaseOnly";

    static {
        System.setProperty(USE_CODEBASE_ONLY, "false");
    }

    PrintStream out;

    public Main(PrintStream out) {
        this.out = out;
    }

    private static void help(String... msgs) {
        if (msgs.length != 0) {
            System.out.println(String.join(System.lineSeparator(), msgs));
            System.out.println();
        }
        System.out.println(String.join(System.lineSeparator(), HELP_MESSAGE));
        System.exit(1);
    }

    private static String indent(int level) {
        StringBuilder buf = new StringBuilder(level * 2);
        for (int i = 0; i < level; i++) {
            buf.append("  ");
        }
        return buf.toString();
    }

    private void list(JmxClient client, List<String> argList) throws IOException {
        Predicate<String> test;
        if (argList.isEmpty()) {
            test = name -> true;
        } else {
            String patternStr = argList.remove(0);
            Pattern pattern = Pattern.compile(patternStr);
            test = name -> pattern.matcher(name).find();
        }
        client.getMBeans().forEach(obj -> {
            ObjectName name = obj.getObjectName();
            if (test.test(name.getCanonicalName())) {
                out.println(obj.getObjectName());
            }
        });
    }

    private void show(JmxClient client, List<String> argList, boolean verbose) throws IOException {
        String objectNameStr = argList.remove(0);
        JmxMap map = client.getMBeanInfo(objectNameStr);
        out.println("{");
        map.walk((walker, elem, isFirst, isLast, parentElemType, parentKeys) -> {
            int level = parentKeys.length + 1;
            String indent = indent(level);
            String comma = isLast ? "" : ",";
            String typeName = "";
            String value = "";
            String description = "";
            if (elem.getElemType() == ElemType.ENTITY) {
                value = JsonUtils.encodeEntity(elem.getValue());
            }
            if (verbose) {
                typeName = String.format("[%s, ", JsonUtils.encodeEntity(elem.getValueTypeName()));
                if (elem.getDescription() == null) {
                    description = "]";
                } else {
                    description = String.format(", %s]", JsonUtils.encodeEntity(elem.getDescription()));
                }
            }
            switch (parentElemType) {
            case ENTITY:
                throw new IllegalStateException();
            case LIST:
                switch (elem.getElemType()) {
                case ENTITY:
                    out.printf("%s%s%s%s%s%n", indent, typeName, value, description, comma);
                    break;
                case LIST:
                    out.printf("%s%s[%n", indent, typeName);
                    elem.walk(walker, parentKeys, elem.getName());
                    out.printf("%s]%s%s%n", indent, description, comma);
                    break;
                case MAP:
                    out.printf("%s%s{%n", indent, typeName);
                    elem.walk(walker, parentKeys, elem.getName());
                    out.printf("%s}%s%s%n", indent, description, comma);
                    break;
                }
                break;
            case MAP:
                String name = JsonUtils.encodeEntity(elem.getName());
                switch (elem.getElemType()) {
                case ENTITY:
                    out.printf("%s%s: %s%s%s%s%n", indent, name, typeName, value, description, comma);
                    break;
                case LIST:
                    out.printf("%s%s: %s[%n", indent, name, typeName);
                    elem.walk(walker, parentKeys, elem.getName());
                    out.printf("%s]%s%s%n", indent, description, comma);
                    break;
                case MAP:
                    out.printf("%s%s: %s{%n", indent, name, typeName);
                    elem.walk(walker, parentKeys, elem.getName());
                    out.printf("%s}%s%s%n", indent, description, comma);
                    break;
                }
                break;
            }
            return true;
        });
        out.println("}");
    }

    public void run(List<String> argList) {
        ListIterator<String> iter = argList.listIterator();
        String host = null;
        int port = -1;
        int pid = -1;
        boolean verbose = false;
        loop: while (iter.hasNext()) {
            String arg = iter.next();
            switch (arg) {
            case "-h":
                iter.remove();
                host = iter.next();
                iter.remove();
                break;
            case "-p":
                iter.remove();
                String portStr = iter.next();
                iter.remove();
                if (!portStr.matches("\\d+")) {
                    abort("Invalid port number: " + portStr);
                }
                port = Integer.parseInt(portStr);
                break;
            case "-P":
                iter.remove();
                String pidStr = iter.next();
                iter.remove();
                if (!pidStr.matches("\\d+")) {
                    abort("Invalid PID: " + pidStr);
                }
                pid = Integer.parseInt(pidStr);
                break;
            case "-v":
                iter.remove();
                verbose = true;
                break;
            default:
                break loop;
            }
        }
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager() {

                @Override
                public void checkPermission(Permission perm) {
                }

                @Override
                public void checkPermission(Permission perm, Object context) {
                }
            });
        }
        String addr;
        if (pid >= 0) {
            addr = JmxClient.getConnectorAddress(pid);
            if (addr == null) {
                help("[ERROR] Cannot attach process: pid=" + pid);
                System.exit(1);
            }
        } else {
            addr = JmxClient.getConnectorAddress(host, port);
        }
        try (JmxClient client = new JmxClient(addr)) {
            String cmd = argList.remove(0);
            switch (cmd) {
            case "list": // list [PATTERN]
                if (argList.size() > 1) {
                    help("[ERROR] Requrired: list [PATTERN]");
                }
                list(client, argList);
                break;

            case "show": // show NAME
                show(client, argList, verbose);
                break;

            default:
                help("[ERROR] Illegal subcommand: " + cmd);
                break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Main of utilities.
     *
     * @param args command line arguments.
     * @throws Exception exception.
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            help();
            return;
        }
        Main main = new Main(System.out);
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        main.run(argList);
    }
}
