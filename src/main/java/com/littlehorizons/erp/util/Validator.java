package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.DocumentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Reusable input checks that throw ValidationException with a clear message.
 * It exists so every controller validates data the same way and no rule is copied between classes.
 */
// SOLID: SRP – this class only validates values; it does not know about the database or the screen
public final class Validator {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9 ]{6,17}$");
    /** TI / CC in Colombia: digits only. */
    private static final Pattern DOCUMENT_DIGITS = Pattern.compile("^[0-9]{5,12}$");
    /** CE / Passport: letters and digits, no spaces. */
    private static final Pattern DOCUMENT_ALNUM = Pattern.compile("^[A-Za-z0-9]{5,20}$");
    /**
     * Person name: Unicode letters (accents), optional spaces, hyphens or apostrophes. No digits.
     */
    private static final Pattern PERSON_NAME =
            Pattern.compile("^[\\p{L}]+(?:[ '\\-][\\p{L}]+)*$");
    private static final Pattern RELATIONSHIP =
            Pattern.compile("^[\\p{L}]+(?:[ ][\\p{L}]+)*$");
    /** Blood type examples: O+, A-, AB+, B+ */
    private static final Pattern BLOOD_TYPE =
            Pattern.compile("^(?i)(O|A|B|AB)[+-]$");

    private static final BigDecimal MIN_SCORE = new BigDecimal("0.0");
    private static final BigDecimal MAX_SCORE = new BigDecimal("5.0");

    private Validator() {
    }

    public static String requireText(String field, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ValidationException(field + " must have at most " + maxLength + " characters.");
        }
        return trimmed;
    }

    public static String optionalText(String field, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireText(field, value, maxLength);
    }

    public static String requirePersonName(String field, String value, int maxLength) {
        String text = requireText(field, value, maxLength);
        if (text.length() < 2) {
            throw new ValidationException(field + " must have at least 2 letters.");
        }
        if (!PERSON_NAME.matcher(text).matches()) {
            throw new ValidationException(field + " must contain only letters (no numbers).");
        }
        return text;
    }

    public static String optionalPersonName(String field, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requirePersonName(field, value, maxLength);
    }

    public static String requireRelationship(String value) {
        String text = requireText("Relationship", value, 50);
        if (!RELATIONSHIP.matcher(text).matches()) {
            throw new ValidationException("Relationship must contain only letters (no numbers).");
        }
        return text;
    }

    public static <T> T requireNotNull(String field, T value) {
        if (value == null) {
            throw new ValidationException(field + " is required.");
        }
        return value;
    }

    public static int requirePositive(String field, int value) {
        if (value <= 0) {
            throw new ValidationException(field + " must be greater than zero.");
        }
        return value;
    }

    /**
     * Document number according to type:
     * TI and CC → only digits (5 to 12).
     * CE and Passport → letters and digits (5 to 20), no spaces.
     */
    public static String requireDocument(DocumentType type, String value) {
        requireNotNull("Document type", type);
        String text = requireText("Document number", value, 20);
        switch (type) {
            case TI:
            case CC:
                if (!DOCUMENT_DIGITS.matcher(text).matches()) {
                    throw new ValidationException(
                            type.getLabel() + " must be only digits (5 to 12 numbers, no letters).");
                }
                break;
            case CE:
            case PASSPORT:
                if (!DOCUMENT_ALNUM.matcher(text).matches()) {
                    throw new ValidationException(
                            type.getLabel() + " must be 5 to 20 letters or digits (no spaces).");
                }
                break;
            default:
                throw new ValidationException("Unsupported document type: " + type);
        }
        return text;
    }

    /**
     * Document without type (search by number). Accepts digits-only or alphanumeric 5–20.
     */
    public static String requireDocument(String value) {
        String text = requireText("Document number", value, 20);
        if (DOCUMENT_DIGITS.matcher(text).matches() || DOCUMENT_ALNUM.matcher(text).matches()) {
            return text;
        }
        throw new ValidationException("Document number must have 5 to 20 letters or digits (no spaces).");
    }

    public static String requireEmail(String value) {
        String text = requireText("Email", value, 100);
        if (!EMAIL.matcher(text).matches()) {
            throw new ValidationException("Email format is not valid.");
        }
        return text;
    }

    public static String optionalEmail(String value) {
        return (value == null || value.isBlank()) ? null : requireEmail(value);
    }

    public static String requirePhone(String field, String value) {
        String text = requireText(field, value, 20);
        String digits = text.replace("+", "").replace(" ", "");
        if (!PHONE.matcher(text).matches() || digits.length() < 7 || digits.length() > 15) {
            throw new ValidationException(field + " must have 7 to 15 digits (optional + at the start).");
        }
        return text;
    }

    public static String optionalPhone(String field, String value) {
        return (value == null || value.isBlank()) ? null : requirePhone(field, value);
    }

    public static String optionalBloodType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim().toUpperCase();
        if (!BLOOD_TYPE.matcher(text).matches()) {
            throw new ValidationException("Blood type must look like O+, A-, AB+, B+.");
        }
        return text;
    }

    public static LocalDate requirePastDate(String field, LocalDate value) {
        requireNotNull(field, value);
        if (!value.isBefore(LocalDate.now())) {
            throw new ValidationException(field + " must be a past date.");
        }
        return value;
    }

    public static LocalDate requireNotFuture(String field, LocalDate value) {
        requireNotNull(field, value);
        if (value.isAfter(LocalDate.now())) {
            throw new ValidationException(field + " cannot be in the future.");
        }
        return value;
    }

    public static BigDecimal requireScore(BigDecimal score) {
        requireNotNull("Score", score);
        if (score.compareTo(MIN_SCORE) < 0 || score.compareTo(MAX_SCORE) > 0) {
            throw new ValidationException("Score must be between 0.0 and 5.0.");
        }
        if (score.stripTrailingZeros().scale() > 1) {
            throw new ValidationException("Score can have only one decimal (for example 4.5).");
        }
        return score.setScale(1);
    }
}
