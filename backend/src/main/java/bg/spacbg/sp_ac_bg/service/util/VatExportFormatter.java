package bg.spacbg.sp_ac_bg.service.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class VatExportFormatter {

    public static final Charset WINDOWS_1251 = Charset.forName("windows-1251");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Converts a UTF-8 string to Base64-encoded Windows-1251 bytes.
     * This is used for NAP VAT export files which require Windows-1251 encoding.
     *
     * @param content The UTF-8 content to encode.
     * @return Base64-encoded string of Windows-1251 bytes.
     */
    public static String toBase64Windows1251(String content) {
        if (content == null) {
            return "";
        }
        byte[] windows1251Bytes = content.getBytes(WINDOWS_1251);
        return Base64.getEncoder().encodeToString(windows1251Bytes);
    }

    /**
     * Formats a string to a fixed length, padding with spaces or truncating.
     * Handles null by returning a string of spaces.
     *
     * @param text   The text to format.
     * @param length The target length.
     * @return The formatted string.
     */
    public static String formatText(String text, int length) {
        if (text == null) {
            return " ".repeat(length);
        }
        if (text.length() > length) {
            return text.substring(0, length);
        }
        return String.format("%-" + length + "s", text);
    }

    /**
     * Formats a BigDecimal amount to a string for the VAT files.
     * The format is a fixed-length string, padded with spaces, with two decimal places but no separator.
     * Example: 123.45 with length 15 becomes "        12345".
     *
     * @param amount The amount to format.
     * @param length The total length of the field.
     * @return The formatted amount string.
     */
    public static String formatAmount(BigDecimal amount, int length) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        String formatted = amount.setScale(2, RoundingMode.HALF_UP).toPlainString().replace(".", "");
        if (formatted.length() > length) {
           // This should not happen with proper column length definitions, but as a fallback
            return formatted.substring(formatted.length() - length);
        }
        return String.format("%" + length + "s", formatted);
    }
    
    /**
    * Formats a BigDecimal amount to a string for the VAT files (DEKLAR.TXT format).
    * The format is a fixed-length string, padded with leading spaces, with two decimal places and a decimal point.
    * Example: 123.45 with length 15 becomes "         123.45".
    *
    * @param amount The amount to format.
    * @param length The total length of the field.
    * @return The formatted amount string.
    */
   public static String formatAmountWithDecimal(BigDecimal amount, int length) {
       if (amount == null) {
           amount = BigDecimal.ZERO;
       }
       String formatted = String.format("%,.2f", amount).trim();
       return String.format("%" + length + "s", formatted);
   }


    /**
     * Formats a LocalDate to dd/MM/yyyy string.
     *
     * @param date The date to format.
     * @return The formatted date string.
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return " ".repeat(10);
        }
        return date.format(DATE_FORMATTER);
    }
}
