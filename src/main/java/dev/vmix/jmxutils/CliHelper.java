/*
 *
 */
package dev.vmix.jmxutils;

@SuppressWarnings("javadoc")
public final class CliHelper {

    private CliHelper() {
    }

    public static void exit() {
        System.exit(0);
    }

    public static void abort(String msg) {
        error(msg);
        System.exit(1);
    }

    public static void info(String msg) {
        System.err.println("[INFO] " + msg);
    }

    public static void error(String msg) {
        System.err.println("[ERROR] " + msg);
    }
}
