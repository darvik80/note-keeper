package xyz.crearts.note.keeper.client;

/**
 * Utility methods for Telegram Bot API MarkdownV2 message formatting.
 * <p>
 * Provides escaping for special characters and convenience builders
 * for common formatting patterns (bold text, labeled fields) so that
 * services don't have to manually concatenate escape calls.
 * </p>
 */
public final class TelegramMarkdownUtil {

    private static final String SPECIAL_CHARS = "_*[]()~`>#+-=|{}.!\\";

    /**
     * Escape special characters for Telegram MarkdownV2 parse mode.
     *
     * @param text raw text
     * @return escaped text safe for MarkdownV2, or empty string if input is null
     */
    public static String escapeMarkdownV2(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (SPECIAL_CHARS.indexOf(c) >= 0) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Wrap text in MarkdownV2 bold markers with proper escaping.
     *
     * @param text text to make bold
     * @return {@code *escaped_text*}
     */
    public static String bold(String text) {
        return "*" + escapeMarkdownV2(text) + "*";
    }

    /**
     * Build a labeled field line: {@code emoji *label:* escaped_value}.
     * <p>Example: {@code field("📅", "Due", "04 Aug 2026, 12:00")}
     * → {@code 📅 *Due:* 04 Aug 2026, 12:00}</p>
     *
     * @param emoji emoji prefix
     * @param label field label (will be bolded and escaped)
     * @param value field value (will be escaped)
     * @return formatted field line
     */
    public static String field(String emoji, String label, String value) {
        return emoji + " " + bold(label) + " " + escapeMarkdownV2(value);
    }
}
