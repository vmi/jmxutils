/*
 *
 */
package dev.vmix.jmxutils;

public final class JsonUtils {

    private JsonUtils() {
    }

    /**
     * Encode entity to JSON element.
     * <p>
     * The following characters are escaped:
     * </p>
     * <ul>
     * <li>'"'
     * <li>'\'
     * <li>'\b'
     * <li>'\f'
     * <li>'\n'
     * <li>'\r'
     * <li>'\t'
     * <li>U+0085 (NEL: Next Line)
     * <li>U+2028 (LS: Line Separator)
     * <li>U+2029 (PS: Paragraph Separator)
     * </ul>
     *
     * @param value source.
     * @return escaped value.
     */
    static String encodeEntity(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }
        String str = value.toString();
        StringBuilder buf = new StringBuilder(str.length() + 16);
        buf.append('"');
        value.toString().chars().forEach(c -> {
            switch (c) {
            case '\\':
            case '"':
                buf.append('\\').append((char) c);
                break;
            case '\b':
                buf.append("\\b");
                break;
            case '\f':
                buf.append("\\f");
                break;
            case '\n':
                buf.append("\\n");
                break;
            case '\r':
                buf.append("\\r");
                break;
            case '\t':
                buf.append("\\t");
                break;
            case '\u0085': // NEL: Next Line
            case '\u2028': // LS: Line Separator
            case '\u2029': // PS: Paragraph Separator
                buf.append(String.format("\\u%04x", c));
                break;
            default:
                buf.append((char) c);
                break;
            }
        });
        buf.append('"');
        return buf.toString();
    }
}
