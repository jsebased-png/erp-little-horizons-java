package com.littlehorizons.erp.service;

import java.awt.image.BufferedImage;

/**
 * Creates a QR code image from a text.
 * It exists so the invoice generator does not depend on a specific QR library.
 */
// SOLID: DIP – the PDF code depends on this interface; ZXing is only one implementation
public interface QrCodeGenerator {

    /** Returns a square image of sizePixels x sizePixels (black modules on white background). */
    BufferedImage generate(String content, int sizePixels);
}
