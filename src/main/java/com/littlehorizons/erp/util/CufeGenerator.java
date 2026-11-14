package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.ErpException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

/**
 * Builds the simulated CUFE (Unique Electronic Invoice Code).
 * The real DIAN algorithm uses more fields and a technical key; here we hash
 * invoice number + NIT + date with SHA-384, which is what the assignment asks for.
 */
// SOLID: SRP – only hashing; it knows nothing about payments or the database
public final class CufeGenerator {

    private CufeGenerator() {
    }

    /** Returns 96 hexadecimal characters (the column cufe holds up to 100). */
    public static String generate(String invoiceNumber, String nit, LocalDate issueDate) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-384");
            String source = invoiceNumber + nit + issueDate;
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-384 is guaranteed by every Java platform, so this should never happen
            throw new ErpException("SHA-384 is not available in this JVM", e);
        }
    }
}
