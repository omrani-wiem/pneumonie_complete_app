package com.example.pneumonia.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

@Component
public class UploadValidator {

    public static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final int MAX_SIDE = 8000;           // pixels
    private static final long MAX_PIXELS = 40_000_000L;
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png");

    public static class InvalidUploadException extends RuntimeException {
        private final boolean unsupportedType;
        public InvalidUploadException(String message, boolean unsupportedType) {
            super(message);
            this.unsupportedType = unsupportedType;
        }
        public boolean isUnsupportedType() { return unsupportedType; }
    }

    /** Retourne le nom de fichier nettoyé, ou lève InvalidUploadException. */
    public String validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new InvalidUploadException("Fichier vide", false);
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidUploadException("Fichier trop volumineux (10 Mo maximum)", false);
        }
        String declared = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!TYPES.contains(declared)) {
            throw new InvalidUploadException("Format non pris en charge : PNG ou JPEG uniquement", true);
        }
        byte[] data = file.getBytes();
        boolean png = startsWith(data, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
        boolean jpeg = startsWith(data, 0xFF, 0xD8, 0xFF);
        if (!png && !jpeg) {
            throw new InvalidUploadException("Le contenu du fichier n'est pas une image PNG ou JPEG valide", true);
        }
        if ((png && !declared.equals("image/png")) || (jpeg && !declared.equals("image/jpeg"))) {
            throw new InvalidUploadException("Le type déclaré ne correspond pas au contenu du fichier", true);
        }
        checkDimensions(data);
        return sanitizeFilename(file.getOriginalFilename());
    }

    private static void checkDimensions(byte[] data) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new InvalidUploadException("Image illisible", true);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                int w = reader.getWidth(0);   // lit l'en-tête seulement, pas toute l'image
                int h = reader.getHeight(0);
                if (w < 32 || h < 32) {
                    throw new InvalidUploadException("Image trop petite", false);
                }
                if (w > MAX_SIDE || h > MAX_SIDE || (long) w * h > MAX_PIXELS) {
                    throw new InvalidUploadException("Dimensions de l'image trop grandes", false);
                }
            } catch (IOException | RuntimeException e) {
                if (e instanceof InvalidUploadException iue) throw iue;
                throw new InvalidUploadException("Image corrompue", true);
            } finally {
                reader.dispose();
            }
        }
    }

    static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "image";
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[\\p{Cntrl}]", "").trim();
        if (base.length() > 100) base = base.substring(base.length() - 100);
        return base.isEmpty() ? "image" : base;
    }

    private static boolean startsWith(byte[] data, int... sig) {
        if (data.length < sig.length) return false;
        for (int i = 0; i < sig.length; i++) {
            if ((data[i] & 0xFF) != sig[i]) return false;
        }
        return true;
    }
}