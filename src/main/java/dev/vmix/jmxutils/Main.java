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

import dev.vmix.jmxutils.models.JmxMap;

/**
 * JMX Utility Main
 */
@SuppressWarnings("javadoc")
public class Main {

    private static final String[] HELP_MESSAGE = {
        "Usage: java -jar jmxutils.jar -h HOST -p PORT COMMAND ARGS ...",
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

    private static boolean isBasicType(String typeName) {
        switch (typeName) {
        case "int":
        case "long":
        case "boolean":
        case "java.lang.String":
            return true;
        default:
            return false;
        }
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

    private void show(JmxClient client, List<String> argList) throws IOException {
        String objectNameStr = argList.remove(0);
        JmxMap map = client.getMBeanInfo(objectNameStr);
        out.println("{");
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
                    out.printf("%s%s%s%n", indent, JsonUtils.encodeEntity(elem.getValue()), comma);
                    break;
                case LIST:
                    out.printf("%s[%n", indent);
                    elem.walk(walker, parentKeys, elem.getName());
                    out.printf("%s]%s%n", indent, comma);
                    break;
                case MAP:
                    out.printf("%s{%n", indent);
                    elem.walk(walker, parentKeys, elem.getName());
                    out.printf("%s}%s%n", indent, comma);
                    break;
                }
                break;
            case MAP:
                switch (elem.getElemType()) {
                case ENTITY:
                    out.printf("%s%s: %s%s%n", indent, JsonUtils.encodeEntity(elem.getName()),
                        JsonUtils.encodeEntity(elem.getValue()), comma);
                    break;
                case LIST:
                    out.printf("%s%s: [%n", indent, JsonUtils.encodeEntity(elem.getName()));
                    elem.walk(walker, parentKeys, elem.getName());
                    out.printf("%s]%s%n", indent, comma);
                    break;
                case MAP:
                    out.printf("%s%s: {%n", indent, JsonUtils.encodeEntity(elem.getName()));
                    elem.walk(walker, parentKeys, elem.getName());
                    out.printf("%s}%s%n", indent, comma);
                    break;
                }
                break;
            }
            return true;
        });
        out.println("}");
    }

    private void showVerbose(JmxClient client, List<String> argList) throws IOException {
        String objectNameStr = argList.remove(0);
        JmxMap map = client.getMBeanInfo(objectNameStr);
        out.println("{");
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
                    out.printf("%s[%s, %s, %s]%s%n", indent,
                        JsonUtils.encodeEntity(elem.getValueTypeName()),
                        JsonUtils.encodeEntity(elem.getValue()),
                        JsonUtils.encodeEntity(elem.getDescription()),
                        comma);
                    break;
                case LIST:
                    out.printf("%s[%s, [%n", indent,
                        JsonUtils.encodeEntity(elem.getValueTypeName()));
                    elem.walk(walker, parentKeys, elem.getName());
                    out.printf("%s], %s]%s%n", indent,
                        JsonUtils.encodeEntity(elem.getDescription()),
                        comma);
                    break;
                case MAP:
                    out.printf("%s[%s, {%n", indent,
                        JsonUtils.encodeEntity(elem.getValueTypeName()));
                    elem.walk(walker, parentKeys, elem.getName());
                    out.printf("%s}, %s]%s%n", indent,
                        JsonUtils.encodeEntity(elem.getDescription()),
                        comma);
                    break;
                }
                break;
            case MAP:
                switch (elem.getElemType()) {
                case ENTITY:
                    out.printf("%s%s: [%s, %s, %s]%s%n", indent,
                        JsonUtils.encodeEntity(elem.getName()),
                        JsonUtils.encodeEntity(elem.getValueTypeName()),
                        JsonUtils.encodeEntity(elem.getValue()),
                        JsonUtils.encodeEntity(elem.getDescription()),
                        comma);
                    break;
                case LIST:
                    out.printf("%s%s: [%s, [%n", indent,
                        JsonUtils.encodeEntity(elem.getName()),
                        JsonUtils.encodeEntity(elem.getValueTypeName()));
                    elem.walk(walker, parentKeys, elem.getName());
                    out.printf("%s], %s]%s%n", indent,
                        JsonUtils.encodeEntity(elem.getDescription()),
                        comma);
                    break;
                case MAP:
                    out.printf("%s%s: [%s, {%n", indent,
                        JsonUtils.encodeEntity(elem.getName()),
                        JsonUtils.encodeEntity(elem.getValueTypeName()));
                    elem.walk(walker, parentKeys, elem.getName());
                    out.printf("%s}, %s]%s%n", indent,
                        JsonUtils.encodeEntity(elem.getDescription()),
                        comma);
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
                showVerbose(client, argList);
                break;

            default:
                help("[ERROR] Illegal subcommand: " + cmd);
                break;
            }
        } catch (IOException e) {
            // TODO 自動生成された catch ブロック
            e.printStackTrace();
        }
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
        Main main = new Main(System.out);
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        main.run(argList);
    }
}
