package dev.vmix.jmxutils;

import java.io.IOException;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.management.ObjectName;

import dev.vmix.jmxutils.models.JmxMap;

/**
 * JMX Utility Main
 */
public class Main {

    private static final String USE_CODEBASE_ONLY = "java.rmi.server.useCodebaseOnly";

    static {
        // String oldValue = System.getProperty(USE_CODEBASE_ONLY);
        System.setProperty(USE_CODEBASE_ONLY, "false");
        // if (!"false".equals(oldValue)) {
        //     System.err.printf("* %s is changed to false.%n", USE_CODEBASE_ONLY);
        // }
    }

    private static void exit() {
        System.exit(0);
    }

    private static void abort(String msg) {
        error(msg);
        System.exit(1);
    }

    private static void info(String msg) {
        System.err.println("[INFO] " + msg);
    }

    private static void error(String msg) {
        System.err.println("[ERROR] " + msg);
    }

    private static void help(String... msgs) {
        String[] help = {
            "Usage: java -jar jmxutils.jar [-h HOST] [-p PORT] COMMAND ARGS ...",
            "",
            "[COMMAND]",
            "list [PATTERN] - MBean一覧の表示 (PATTERNは正規表現)",
            "show NAME      - MBean情報の表示"
        };
        if (msgs.length != 0) {
            System.out.println(String.join(System.lineSeparator(), msgs));
            System.out.println();
            System.out.println();
        }
        System.out.println(String.join(System.lineSeparator(), help));
        System.out.println();
        System.exit(1);
    }

    //    private static Properties loadProps(String... file) {
    //        Properties props = new Properties();
    //        InputStream is;
    //        try {
    //            if (file.length == 0) {
    //                is = Main.class.getResourceAsStream("/jmxutils.properties");
    //            } else {
    //                is = new FileInputStream(file[1]);
    //            }
    //            props.load(is);
    //            return props;
    //        } catch (FileNotFoundException e) {
    //            abort("File not found: " + file);
    //            // not reached.
    //            throw new RuntimeException(e);
    //        } catch (IOException e) {
    //            throw new RuntimeException(e);
    //        }
    //    }

    private static String escape(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        } else {
            return "\"" + value.toString().replaceAll("([\"\\\\])", "\\$1") + "\"";
        }
    }

    private static String indent(int level) {
        StringBuilder buf = new StringBuilder(level * 2);
        for (int i = 0; i < level; i++) {
            buf.append("  ");
        }
        return buf.toString();
    }

    private static void list(JmxClient client, List<String> argList) throws IOException {
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
                System.out.println(obj.getObjectName());
            }
        });
    }

    private static void show(JmxClient client, List<String> argList) throws IOException {
        String objectNameStr = argList.remove(0);
        JmxMap map = client.getMBeanInfo(objectNameStr);
        System.out.println("{");
        map.walk((walker, elem, isFirst, isLast, parentElemType, parentKeys) -> {
            int level = parentKeys.length + 1;
            String indent = indent(level);
            String comma = isLast ? "" : ",";
            switch (parentElemType) {
            case ENTITY:
                throw new IllegalStateException();
            case LIST:
                switch (elem.getElemType()) {
                case ENTITY:
                    System.out.printf("%s%s%s%n", indent, escape(elem.getValue()), comma);
                    break;
                case LIST:
                    System.out.printf("%s[%n", indent);
                    elem.walk(walker, parentKeys, elem.getName());
                    System.out.printf("%s]%s%n", indent, comma);
                    break;
                case MAP:
                    System.out.printf("%s{%n", indent);
                    elem.walk(walker, parentKeys, elem.getName());
                    System.out.printf("%s}%s%n", indent, comma);
                    break;
                }
                break;
            case MAP:
                switch (elem.getElemType()) {
                case ENTITY:
                    System.out.printf("%s%s: %s%s%n", indent, escape(elem.getName()), escape(elem.getValue()), comma);
                    break;
                case LIST:
                    System.out.printf("%s%s: [%n", indent, escape(elem.getName()));
                    elem.walk(walker, parentKeys, elem.getName());
                    System.out.printf("%s]%s%n", indent, comma);
                    break;
                case MAP:
                    System.out.printf("%s%s: {%n", indent, escape(elem.getName()));
                    elem.walk(walker, parentKeys, elem.getName());
                    System.out.printf("%s}%s%n", indent, comma);
                    break;
                }
                break;
            }
            return true;
        });
        System.out.println("}");
    }

    /**
     * Main of utilities.
     *
     * @param args command line arguments.
     * @throws Exception exception.
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            help();
            return;
        }
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        ListIterator<String> iter = argList.listIterator();
        String host = null;
        int port = -1;
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
        try (JmxClient client = new JmxClient(host, port)) {
            String cmd = argList.remove(0);
            switch (cmd) {
            case "list": // list [PATTERN]
                if (argList.size() > 1) {
                    help("[ERROR] Requrired: list [PATTERN]");
                }
                list(client, argList);
                break;

            case "show": // show NAME
                show(client, argList);
                break;

            default:
                help("[ERROR] Illegal subcommand: " + cmd);
                break;
            }
        }

        //client.getMBeanInfo("java.lang:type=GarbageCollector,name=ConcurrentMarkSweep");
        //"java.lang:type=GarbageCollector,name=ConcurrentMarkSweep.LastGcInfo.memoryUsageAfterGc.CMS Old Gen.max");
        //            client.getMBeanInfo(
        //                "java.lang:type=GarbageCollector,name=ConcurrentMarkSweep.LastGcInfo.memoryUsageAfterGc.CMS Old Gen.used");
    }
}
