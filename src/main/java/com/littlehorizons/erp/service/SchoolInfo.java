package com.littlehorizons.erp.service;

import com.littlehorizons.erp.util.ConfigLoader;

/**
 * School data printed on every invoice (name, NIT, address, phone, email, DIAN resolution).
 * It exists so the PDF generator receives plain values and never reads config.properties by itself.
 */
public record SchoolInfo(String name, String nit, String address, String phone, String email,
                         String dianResolution) {

    /** Reads the school.* keys of config.properties. Missing keys fail with a clear ConfigurationException. */
    public static SchoolInfo fromConfig(ConfigLoader config) {
        return new SchoolInfo(
                config.get("school.name"),
                config.get("school.nit"),
                config.get("school.address"),
                config.get("school.phone"),
                config.get("school.email"),
                config.get("school.dian.resolution"));
    }
}
