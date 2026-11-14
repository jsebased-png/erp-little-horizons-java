package com.littlehorizons.erp.view.console;

import com.littlehorizons.erp.exception.InputClosedException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Reads and validates what the user types. Every method repeats the question until the answer has a valid FORMAT
 * (a number, a date, a value inside a range, a non-empty text), so the program never fails because of a typing mistake.
 * Business rules (is the club full? is the grade valid?) are NOT checked here: they belong to the controllers.
 */
// SOLID: SRP – input and format validation only
public class ConsoleInput {

    /** Day/month/year, strict: 31/02/2026 is rejected instead of being "fixed" silently. */
    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final Scanner scanner;
    private final ConsolePrinter printer;

    public ConsoleInput(Scanner scanner, ConsolePrinter printer) {
        this.scanner = scanner;
        this.printer = printer;
    }

    // ------------------------------------------------------------------ raw line

    /** Shows the prompt and returns the typed line (may be empty). The only place that touches the Scanner. */
    public String readLine(String prompt) {
        printer.print("  » " + prompt + ": ");
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException e) {
            throw new InputClosedException();
        }
    }

    // ------------------------------------------------------------------ text

    /** Required text: asks again while the user types nothing. */
    public String readText(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            printer.warning("Este dato es obligatorio.");
        }
    }

    /** Optional text: ENTER returns null. */
    public String readOptionalText(String prompt) {
        String value = readLine(prompt + " (ENTER para omitir)");
        return value.isEmpty() ? null : value;
    }

    /**
     * Required person name: only letters (accents OK). Rejects digits. Asks again until valid.
     */
    public String readPersonName(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return Validator.requirePersonName(prompt, value, 100);
            } catch (ValidationException e) {
                printer.warning("Solo letras (sin números). Ejemplo: María, José.");
            }
        }
    }

    /** Optional person name: ENTER → null; if typed, must be letters only. */
    public String readOptionalPersonName(String prompt) {
        while (true) {
            String value = readLine(prompt + " (ENTER para omitir)");
            if (value.isEmpty()) {
                return null;
            }
            try {
                return Validator.requirePersonName(prompt, value, 100);
            } catch (ValidationException e) {
                printer.warning("Solo letras (sin números). Ejemplo: María, José.");
            }
        }
    }

    /** Required phone: digits only (optional +). Asks again until valid. */
    public String readPhone(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return Validator.requirePhone(prompt, value);
            } catch (ValidationException e) {
                printer.warning("Teléfono inválido. Use 7 a 15 dígitos (ej. 3001234567).");
            }
        }
    }

    /** Optional phone: ENTER → null. */
    public String readOptionalPhone(String prompt) {
        while (true) {
            String value = readLine(prompt + " (ENTER para omitir)");
            if (value.isEmpty()) {
                return null;
            }
            try {
                return Validator.requirePhone(prompt, value);
            } catch (ValidationException e) {
                printer.warning("Teléfono inválido. Use 7 a 15 dígitos (ej. 3001234567).");
            }
        }
    }

    /** Required email with format check. */
    public String readEmail(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return Validator.requireEmail(value);
            } catch (ValidationException e) {
                printer.warning("Correo inválido. Ejemplo: nombre@correo.com");
            }
        }
    }

    /** Optional email: ENTER → null. */
    public String readOptionalEmail(String prompt) {
        while (true) {
            String value = readLine(prompt + " (ENTER para omitir)");
            if (value.isEmpty()) {
                return null;
            }
            try {
                return Validator.requireEmail(value);
            } catch (ValidationException e) {
                printer.warning("Correo inválido. Ejemplo: nombre@correo.com");
            }
        }
    }

    /** Relationship without digits (Padre, Madre, Tutor legal). */
    public String readRelationship(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return Validator.requireRelationship(value);
            } catch (ValidationException e) {
                printer.warning("Solo letras. Ejemplo: Madre, Padre, Tío, Tutor legal.");
            }
        }
    }

    /**
     * Document number validated by type: TI/CC only digits; CE/Passport letters+digits.
     * Asks again until the format is correct.
     */
    public String readDocument(DocumentType type, String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return Validator.requireDocument(type, value);
            } catch (ValidationException e) {
                if (type == DocumentType.TI || type == DocumentType.CC) {
                    printer.warning(type + " solo admite números (5 a 12 dígitos). Ejemplo: 1001234567");
                } else {
                    printer.warning(type + " admite letras y números (5 a 20). Ejemplo: AB123456");
                }
            }
        }
    }

    public String readOptionalBloodType(String prompt) {
        while (true) {
            String value = readLine(prompt + " (ENTER para omitir)");
            if (value.isEmpty()) {
                return null;
            }
            try {
                return Validator.optionalBloodType(value);
            } catch (ValidationException e) {
                printer.warning("Tipo de sangre inválido. Ejemplos: O+, A-, AB+, B+");
            }
        }
    }

    public String readBloodTypeOrKeep(String prompt, String current) {
        while (true) {
            String value = readLine(prompt + " [" + (current == null || current.isBlank() ? "vacío" : current) + "]");
            if (value.isEmpty()) {
                return current;
            }
            try {
                return Validator.optionalBloodType(value);
            } catch (ValidationException e) {
                printer.warning("Tipo de sangre inválido. Ejemplos: O+, A-, AB+, B+");
            }
        }
    }




    /** Used when editing: ENTER keeps the current value. */
    public String readTextOrKeep(String prompt, String current) {
        String value = readLine(prompt + " [" + (current == null ? "-" : current) + "]");
        return value.isEmpty() ? current : value;
    }

    /** Like readTextOrKeep but the new value must be a person name (letters only). */
    public String readPersonNameOrKeep(String prompt, String current) {
        while (true) {
            String value = readLine(prompt + " [" + (current == null || current.isBlank() ? "vacío" : current) + "]");
            if (value.isEmpty()) {
                return current;
            }
            try {
                return Validator.requirePersonName(prompt, value, 100);
            } catch (ValidationException e) {
                printer.warning("Solo letras (sin números). Ejemplo: María, José.");
            }
        }
    }

    /** Keep current phone or validate a new one. */
    public String readPhoneOrKeep(String prompt, String current) {
        while (true) {
            String value = readLine(prompt + " [" + (current == null || current.isBlank() ? "vacío" : current) + "]");
            if (value.isEmpty()) {
                return current;
            }
            try {
                return Validator.requirePhone(prompt, value);
            } catch (ValidationException e) {
                printer.warning("Teléfono inválido. Use 7 a 15 dígitos (ej. 3001234567).");
            }
        }
    }

    /** Keep current email or validate a new one. */
    public String readEmailOrKeep(String prompt, String current) {
        while (true) {
            String value = readLine(prompt + " [" + (current == null || current.isBlank() ? "vacío" : current) + "]");
            if (value.isEmpty()) {
                return current;
            }
            try {
                return Validator.requireEmail(value);
            } catch (ValidationException e) {
                printer.warning("Correo inválido. Ejemplo: nombre@correo.com");
            }
        }
    }



    // ------------------------------------------------------------------ numbers

    /** Whole number between min and max (both included). */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            String value = readLine(prompt);
            try {
                int number = Integer.parseInt(value);
                if (number >= min && number <= max) {
                    return number;
                }
                printer.warning("Ingrese un número entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) {
                printer.warning("Ingrese un número entero válido.");
            }
        }
    }

    /** Whole number, ENTER returns the default value. */
    public int readIntOrDefault(String prompt, int min, int max, int defaultValue) {
        while (true) {
            String value = readLine(prompt + " [" + defaultValue + "]");
            if (value.isEmpty()) {
                return defaultValue;
            }
            try {
                int number = Integer.parseInt(value);
                if (number >= min && number <= max) {
                    return number;
                }
                printer.warning("Ingrese un número entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) {
                printer.warning("Ingrese un número entero válido.");
            }
        }
    }

    /** Decimal number; accepts "4.5" and "4,5". */
    public BigDecimal readDecimal(String prompt, double min, double max) {
        while (true) {
            String value = readLine(prompt).replace(',', '.');
            try {
                BigDecimal number = new BigDecimal(value);
                if (number.doubleValue() >= min && number.doubleValue() <= max) {
                    return number;
                }
                printer.warning("Ingrese un valor entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) {
                printer.warning("Ingrese un número válido (por ejemplo 4.5).");
            }
        }
    }

    // ------------------------------------------------------------------ dates and times

    /** Date in dd/MM/yyyy. */
    public LocalDate readDate(String prompt) {
        while (true) {
            String value = readLine(prompt + " (dd/mm/aaaa)");
            try {
                return LocalDate.parse(value, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                printer.warning("Fecha no válida. Use el formato dd/mm/aaaa, por ejemplo 15/03/2026.");
            }
        }
    }

    /** Date, ENTER returns the default (usually today). */
    public LocalDate readDateOrDefault(String prompt, LocalDate defaultValue) {
        while (true) {
            String value = readLine(prompt + " (dd/mm/aaaa) [" + defaultValue.format(DATE_FORMAT) + "]");
            if (value.isEmpty()) {
                return defaultValue;
            }
            try {
                return LocalDate.parse(value, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                printer.warning("Fecha no válida. Use el formato dd/mm/aaaa.");
            }
        }
    }

    /** Optional date: ENTER returns null. */
    public LocalDate readOptionalDate(String prompt) {
        while (true) {
            String value = readLine(prompt + " (dd/mm/aaaa, ENTER para omitir)");
            if (value.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(value, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                printer.warning("Fecha no válida. Use el formato dd/mm/aaaa.");
            }
        }
    }

    /** Time in 24-hour format HH:mm. */
    public LocalTime readTime(String prompt) {
        while (true) {
            String value = readLine(prompt + " (HH:mm)");
            try {
                return LocalTime.parse(value, TIME_FORMAT);
            } catch (DateTimeParseException e) {
                printer.warning("Hora no válida. Use el formato HH:mm, por ejemplo 07:30.");
            }
        }
    }

    // ------------------------------------------------------------------ choices

    /** Yes/no question. Accepts s, si, y, yes / n, no. */
    public boolean confirm(String question) {
        while (true) {
            String value = readLine(question + " (s/n)").toLowerCase();
            if (value.equals("s") || value.equals("si") || value.equals("sí") || value.equals("y") || value.equals("yes")) {
                return true;
            }
            if (value.equals("n") || value.equals("no")) {
                return false;
            }
            printer.warning("Responda con s (sí) o n (no).");
        }
    }

    /** Shows a numbered list and returns the option the user picked. */
    public <T> T readChoice(String prompt, List<T> options, Function<T, String> label) {
        for (int i = 0; i < options.size(); i++) {
            printer.println("     " + (i + 1) + ". " + label.apply(options.get(i)));
        }
        int number = readInt(prompt, 1, options.size());
        return options.get(number - 1);
    }

    /** Same as readChoice but for every constant of an enum (uses its toString, which is the Spanish label). */
    public <E extends Enum<E>> E readEnum(String prompt, Class<E> type) {
        return readChoice(prompt, List.of(type.getEnumConstants()), E::toString);
    }

    /** Waits for ENTER so the user can read the screen before the menu is drawn again. */
    public void pause() {
        readLine("Presione ENTER para continuar");
    }
}
