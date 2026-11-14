package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Reads config.properties once and exposes its values.
 * It exists so credentials and school data live outside the source code.
 */
// SOLID: SRP – this class only reads configuration; it knows nothing about databases or invoices
public final class ConfigLoader {

    private static final Path CONFIG_PATH = Paths.get("config.properties");

    private final Properties properties = new Properties();

    /** Loads the file using java.nio.file and try-with-resources. */
    public ConfigLoader() {
        if (!Files.exists(CONFIG_PATH)) {
            throw new ConfigurationException(
                    "config.properties not found. Copy config.properties.example and fill in your values.");
        }
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            properties.load(in);
        } catch (IOException e) {
            throw new ConfigurationException("Could not read config.properties", e);
        }
    }

    /** Returns a required value or fails with a clear message. */
    public String get(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new ConfigurationException("Missing required property: " + key);
        }
        return value.trim();
    }

    /** Returns an optional value or the given default. */
    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue).trim();
    }
}
